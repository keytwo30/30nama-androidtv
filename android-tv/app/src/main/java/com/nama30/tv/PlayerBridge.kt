package com.nama30.tv

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

/** Native ExoPlayer behind the transparent WebView, driven through the webapis.avplay shim. */
@OptIn(UnstableApi::class)
class PlayerBridge(
    private val ctx: Context,
    private val web: WebView,
    private val surface: SurfaceView,
    private val http: OkHttpClient,
) {
    private val main = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var url: String? = null
    private var prepared = false
    private var selector: DefaultTrackSelector? = null
    @Volatile private var tracksJson = "[]"

    private val ticker = object : Runnable {
        override fun run() {
            val p = player ?: return
            js("__av.time(${p.currentPosition},${dur(p)})")
            main.postDelayed(this, 250)
        }
    }

    private fun dur(p: Player) = if (p.duration == C.TIME_UNSET) 0 else p.duration

    private fun js(code: String) = web.evaluateJavascript(code, null)

    private fun build(): ExoPlayer {
        val sel = DefaultTrackSelector(ctx)
        selector = sel
        val ds = OkHttpDataSource.Factory(http).setUserAgent(Net.UA)
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(20_000, 90_000, 1_500, 3_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        // Start at a high rendition instead of ramping up from SD.
        val meter = DefaultBandwidthMeter.Builder(ctx).setInitialBitrateEstimate(25_000_000L).build()
        val renderers = DefaultRenderersFactory(ctx).setEnableDecoderFallback(true)
        return ExoPlayer.Builder(ctx, renderers)
            .setTrackSelector(sel)
            .setLoadControl(load)
            .setBandwidthMeter(meter)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ds))
            .build().apply {
                setVideoSurfaceView(surface)
                addListener(listener)
            }
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val p = player ?: return
            when (state) {
                Player.STATE_BUFFERING -> if (prepared) js("__av.buffering(true)")
                Player.STATE_READY -> {
                    if (!prepared) { prepared = true; js("__av.prepared(${dur(p)})") }
                    else js("__av.buffering(false)")
                }
                Player.STATE_ENDED -> js("__av.ended()")
                else -> {}
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            surface.keepScreenOn = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            js("__av.error(${JSONObject.quote(error.errorCodeName + " " + (error.cause?.message ?: ""))})")
        }

        override fun onVideoSizeChanged(size: VideoSize) = fit(size)

        override fun onTracksChanged(tracks: Tracks) {
            val arr = JSONArray()
            var i = 0
            for (g in tracks.groups) {
                if (g.type != C.TRACK_TYPE_AUDIO) continue
                val f = g.mediaTrackGroup.getFormat(0)
                arr.put(JSONObject().put("index", i++).put("type", "AUDIO")
                    .put("extra_info", JSONObject().put("language", f.language ?: f.label ?: "").toString()))
            }
            tracksJson = arr.toString()
        }
    }

    /** Letterbox the surface to the video's aspect ratio. */
    private fun fit(size: VideoSize) {
        if (size.width == 0 || size.height == 0) return
        val parent = surface.parent as View
        val pw = parent.width.toFloat(); val ph = parent.height.toFloat()
        if (pw == 0f || ph == 0f) return
        val ar = size.width * size.pixelWidthHeightRatio / size.height
        val (w, h) = if (pw / ph > ar) (ph * ar) to ph else pw to (pw / ar)
        surface.layoutParams = (surface.layoutParams as FrameLayout.LayoutParams).apply {
            width = w.toInt(); height = h.toInt()
        }
    }

    private fun ui(block: () -> Unit) { main.post(block) }

    @JavascriptInterface fun open(u: String) = ui { url = u }

    @JavascriptInterface fun prepare() = ui {
        val u = url ?: return@ui
        val p = player ?: build().also { player = it }
        prepared = false
        surface.visibility = View.VISIBLE
        val item = MediaItem.Builder().setUri(Uri.parse(u))
            .apply { if (!u.contains(".mp4", true)) setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8) }
            .build()
        p.setMediaItem(item)
        p.prepare()
        main.removeCallbacks(ticker); main.post(ticker)
    }

    @JavascriptInterface fun play() = ui { player?.play() }
    @JavascriptInterface fun pause() = ui { player?.pause() }
    @JavascriptInterface fun seekTo(ms: Long) = ui { player?.seekTo(ms) }
    @JavascriptInterface fun tracks(): String = tracksJson

    @JavascriptInterface fun selectAudio(index: Int) = ui {
        val p = player ?: return@ui
        val groups = p.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val g = groups.getOrNull(index) ?: return@ui
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0))
            .build()
    }

    @JavascriptInterface fun stop() = ui { player?.stop() }
    @JavascriptInterface fun close() = ui { release() }

    fun pauseIfPlaying() { player?.pause() }

    fun release() {
        main.removeCallbacks(ticker)
        player?.release(); player = null
        tracksJson = "[]"; prepared = false
        surface.visibility = View.GONE
        surface.keepScreenOn = false
    }
}
