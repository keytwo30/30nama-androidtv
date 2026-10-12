package com.nama30.tv.player

import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.media3.common.C as MC
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
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
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nama30.tv.Net
import com.nama30.tv.data.Api
import com.nama30.tv.data.Prefs
import com.nama30.tv.data.StreamItem
import com.nama30.tv.ui.Btn
import com.nama30.tv.ui.C
import com.nama30.tv.ui.Clock
import com.nama30.tv.ui.EpisodeTile
import com.nama30.tv.ui.Nav
import com.nama30.tv.ui.PlayRequest
import com.nama30.tv.ui.R10
import com.nama30.tv.ui.R6
import com.nama30.tv.ui.Spinner
import com.nama30.tv.ui.T
import com.nama30.tv.ui.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class AudioTrack(val index: Int, val label: String, val selected: Boolean)

private val LANGS = mapOf("eng" to "English", "en" to "English", "per" to "Persian", "fas" to "Persian", "fa" to "Persian", "fre" to "French", "fra" to "French",
    "ger" to "German", "deu" to "German", "spa" to "Spanish", "jpn" to "Japanese", "ja" to "Japanese", "kor" to "Korean", "tur" to "Turkish", "ara" to "Arabic", "rus" to "Russian")
private fun langName(l: String?): String? = l?.lowercase()?.let { LANGS[it] ?: LANGS[it.take(3)] ?: l }

private fun fmt(s: Double): String {
    val x = s.coerceAtLeast(0.0).toLong(); val h = x / 3600; val m = x % 3600 / 60; val sec = x % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@OptIn(UnstableApi::class)
private fun buildPlayer(ctx: android.content.Context): ExoPlayer {
    val ds = OkHttpDataSource.Factory(Net.client(ctx.cacheDir)).setUserAgent(Net.UA)
    val load = DefaultLoadControl.Builder()
        .setBufferDurationsMs(20_000, 60_000, 1_500, 3_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
    // Start at a high rendition instead of ramping up from SD.
    val meter = DefaultBandwidthMeter.Builder(ctx).setInitialBitrateEstimate(25_000_000L).build()
    return ExoPlayer.Builder(ctx, DefaultRenderersFactory(ctx).setEnableDecoderFallback(true))
        .setLoadControl(load).setBandwidthMeter(meter)
        .setMediaSourceFactory(DefaultMediaSourceFactory(ds))
        .build()
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(req: PlayRequest) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { buildPlayer(ctx) }
    val items = req.items
    var index by remember { mutableIntStateOf(req.index) }
    val item = items[index]

    var pos by remember { mutableDoubleStateOf(0.0) }
    var dur by remember { mutableDoubleStateOf(0.0) }
    var buf by remember { mutableDoubleStateOf(0.0) }
    var paused by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var aspect by remember { mutableFloatStateOf(16f / 9f) }
    var osd by remember { mutableStateOf(true) }
    var osdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var seekAcc by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var eps by remember { mutableStateOf(false) }
    var skipFocus by remember { mutableStateOf(false) }
    var upNext by remember { mutableStateOf<String?>(null) }   // null | shown | active | dismissed
    var upSel by remember { mutableStateOf("play") }
    var audio by remember { mutableStateOf<List<AudioTrack>>(emptyList()) }
    var subLang by remember { mutableStateOf("off") }
    var cues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    val subCache = remember { HashMap<String, List<Cue>>() }
    var resume by remember { mutableStateOf(req.resume) }
    val rootFr = remember { FocusRequester() }

    fun showOsd() { osd = true; osdAt = System.currentTimeMillis() }
    fun report() { val p = pos; val d = dur; val it = items[index]; if (p > 1) scope.launch { Api.observer(req.post.id, it, p, d) } }
    fun close() { report(); Nav.player = null }

    fun setSub(lang: String) {
        subLang = lang; cues = emptyList()
        val u = items[index].subs[lang] ?: return
        subCache[u]?.let { cues = it; return }
        scope.launch {
            try {
                val c = parseSubs(Api.subtitle(u))
                subCache[u] = c; if (subLang == lang) cues = c
            } catch (e: Exception) { android.util.Log.w("n30", "sub failed", e); Nav.showToast(t("subFailed")) }
        }
    }

    fun playIndex(i: Int) {
        report()
        index = i; upNext = null; skipFocus = false; eps = false; menu = false; seekAcc = 0
        val it = items[i]
        val url = it.bestUrl
        if (url == null) { Nav.showToast(t("noStream")); Nav.player = null; return }
        val start = resume?.takeIf { r -> r.fileId == it.fileId && r.time > 5 }?.time ?: 0.0
        resume = null
        player.setMediaItem(MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build(), (start * 1000).toLong())
        player.prepare(); player.playWhenReady = true
        paused = false; buffering = true; pos = start; dur = 0.0
        val want = Prefs.defaultSub
        setSub(if (want != "off" && it.subs.containsKey(want)) want else "off")
        showOsd()
    }

    fun seekBy(sec: Int) { seekAcc += sec; showOsd() }
    fun togglePause() { paused = !paused; player.playWhenReady = !paused; showOsd(); if (paused) report() }

    // player lifecycle + listeners
    DisposableEffect(Unit) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) { if (index + 1 < items.size) playIndex(index + 1) else close() }
            }
            override fun onVideoSizeChanged(v: VideoSize) { if (v.width > 0 && v.height > 0) aspect = v.width * v.pixelWidthHeightRatio / v.height }
            override fun onPlayerError(e: PlaybackException) { buffering = false; Nav.showToast("${t("playErr")}: ${e.errorCodeName}"); showOsd() }
            override fun onTracksChanged(tr: Tracks) {
                audio = tr.groups.filter { it.type == MC.TRACK_TYPE_AUDIO }.mapIndexed { i, g ->
                    val f = g.mediaTrackGroup.getFormat(0)
                    AudioTrack(i, langName(f.language) ?: f.label ?: "${t("audio")} ${i + 1}", g.isSelected)
                }
            }
        }
        player.addListener(l)
        playIndex(index)
        onDispose { player.removeListener(l); player.release() }
    }
    // pause when the app leaves the screen
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP && !paused) { paused = true; player.playWhenReady = false; report() } }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // clock: position, subtitles, skip/up-next triggers
    LaunchedEffect(Unit) {
        while (true) {
            pos = player.currentPosition / 1000.0
            dur = player.duration.takeIf { it != MC.TIME_UNSET }?.div(1000.0) ?: 0.0
            buf = player.bufferedPosition / 1000.0
            val hasNext = index + 1 < items.size
            val c = items[index].creditsAt
            if (hasNext && c > 0 && pos >= c && upNext == null) upNext = "shown"
            else if (upNext != null && upNext != "dismissed" && c > 0 && pos < c - 3) upNext = null
            if (osd && !paused && !menu && !eps && seekAcc == 0 && System.currentTimeMillis() - osdAt > 4000) osd = false
            delay(200)
        }
    }
    // seek: accumulate presses, apply after a short pause
    LaunchedEffect(seekAcc) {
        if (seekAcc == 0) return@LaunchedEffect
        delay(700)
        val target = (pos + seekAcc).coerceIn(0.0, if (dur > 0) dur - 2 else Double.MAX_VALUE)
        player.seekTo((target * 1000).toLong()); pos = target; seekAcc = 0
    }
    // progress heartbeat
    LaunchedEffect(index) { while (true) { delay(10_000); if (!paused) report() } }
    // up next autoplay countdown
    LaunchedEffect(upNext) {
        if ((upNext == "shown" || upNext == "active") && Prefs.autoplay) { delay(10_000); if (upNext == "shown" || upNext == "active") playIndex(index + 1) }
    }

    val skip: Pair<String, Double>? = when {
        item.introEnd > 0 && pos >= item.introStart && pos < item.introEnd - 1 -> t("skipIntro") to item.introEnd
        item.recapEnd > 0 && pos >= item.recapStart && pos < item.recapEnd - 1 -> t("skipRecap") to item.recapEnd
        else -> null
    }
    if (skip == null && skipFocus) skipFocus = false
    val upShown = upNext == "shown" || upNext == "active"

    BackHandler {
        when {
            menu -> menu = false
            eps -> eps = false
            skipFocus -> skipFocus = false
            upNext == "active" -> upNext = "shown"
            osd || upShown -> { osd = false; if (upShown) upNext = "dismissed" }
            else -> close()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black).focusRequester(rootFr).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || menu || eps) return@onPreviewKeyEvent false
                val toEnd = if (com.nama30.tv.ui.rtl) Key.DirectionLeft else Key.DirectionRight
                when (e.key) {
                    Key.MediaPlayPause -> { togglePause(); true }
                    Key.MediaPlay -> { if (paused) togglePause(); true }
                    Key.MediaPause -> { if (!paused) togglePause(); true }
                    Key.MediaStop -> { close(); true }
                    Key.MediaFastForward, Key.MediaNext -> { seekBy(30); true }
                    Key.MediaRewind, Key.MediaPrevious -> { seekBy(-30); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        when {
                            skipFocus && skip != null -> { player.seekTo((skip.second * 1000).toLong()); skipFocus = false }
                            upNext == "active" -> { if (upSel == "play") playIndex(index + 1) else upNext = "dismissed" }
                            else -> togglePause()
                        }; true
                    }
                    Key.DirectionUp -> {
                        when {
                            upShown && upNext != "active" -> { upNext = "active"; upSel = "play" }
                            upNext == "active" -> upNext = "shown"
                            skip != null && !skipFocus -> skipFocus = true
                            else -> { skipFocus = false; menu = true; showOsd() }
                        }; true
                    }
                    Key.DirectionDown -> {
                        when {
                            upNext == "active" -> upNext = "shown"
                            skipFocus -> skipFocus = false
                            items.size > 1 -> { eps = true; showOsd() }
                        }; true
                    }
                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (upNext == "active") upSel = if (upSel == "play") "cred" else "play"
                        else { skipFocus = false; seekBy(if (e.key == toEnd) 10 else -10) }
                        true
                    }
                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it); it.keepScreenOn = true } },
            modifier = Modifier.align(Alignment.Center).aspectRatio(aspect, matchHeightConstraintsFirst = aspect < 16f / 9f),
        )

        // subtitles
        val sub = if (cues.isEmpty()) "" else cueAt(cues, pos)
        if (sub.isNotEmpty()) Text(
            sub, textAlign = TextAlign.Center,
            style = T.Body.copy(fontSize = (Prefs.subSize / 2).sp, fontWeight = FontWeight.Medium, lineHeight = (Prefs.subSize / 2 * 1.35).sp,
                shadow = Shadow(Color.Black, blurRadius = 6f), textDirection = TextDirection.Content),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (osd) 96.dp else 40.dp, start = 80.dp, end = 80.dp),
        )

        // OSD
        AnimatedVisibility(osd, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(130.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .75f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .85f)))))
                Column(Modifier.padding(start = 48.dp, top = 30.dp)) {
                    Text(req.post.title(Prefs.lang), style = T.H1)
                    if (items.size > 1 || req.post.isSeries) Text("S${item.season} · E${item.epLabel()}" + (item.name?.let { " — $it" } ?: ""), style = T.Body.copy(color = C.Text2))
                }
                Clock(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 48.dp))
                Column(Modifier.align(Alignment.BottomCenter).padding(start = 48.dp, end = 48.dp, bottom = 24.dp)) {
                    val shownPos = if (seekAcc != 0 && dur > 0) (pos + seekAcc).coerceIn(0.0, dur) else pos
                    if (seekAcc != 0) Text("${fmt(shownPos)}   ${if (seekAcc > 0) "+" else ""}${seekAcc}s", style = T.H2, modifier = Modifier.padding(bottom = 8.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = .2f), R6)) {
                        if (dur > 0) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((buf / dur).toFloat().coerceIn(0f, 1f)).background(Color.White.copy(alpha = .3f), R6))
                            Box(Modifier.fillMaxHeight().fillMaxWidth((shownPos / dur).toFloat().coerceIn(0f, 1f)).background(C.Accent, R6))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(fmt(shownPos), style = T.Body)
                        Spacer(Modifier.weight(1f))
                        if (dur > 0) Text("-" + fmt(dur - shownPos), style = T.Body)
                    }
                    Text(if (items.size > 1) t("hintPlayerEps") else t("hintPlayer"), style = T.Small.copy(color = C.Text2), modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
                }
            }
        }

        if (paused) Icon(Icons.Default.Pause, null, tint = Color.White.copy(alpha = .85f), modifier = Modifier.align(Alignment.Center).size(72.dp))
        if (buffering && !paused) Box(Modifier.align(Alignment.Center)) { Spinner(44.dp) }

        // skip intro / recap pill
        if (skip != null && !menu && !eps) Text(
            if (skipFocus) "OK  ·  ${skip.first}  ⏭" else "▲  ${skip.first}",
            style = T.Btn.copy(color = if (skipFocus) C.Bg else C.Text1),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = if (osd) 150.dp else 48.dp)
                .background(if (skipFocus) C.Text1 else Color.Black.copy(alpha = .6f), R6).padding(horizontal = 16.dp, vertical = 9.dp),
        )

        // up next card
        if (upShown && index + 1 < items.size) {
            val next = items[index + 1]; val active = upNext == "active"
            Column(Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = if (osd) 150.dp else 48.dp).width(300.dp)
                .background(C.Surface.copy(alpha = .95f), R10).padding(16.dp)) {
                Text(t("nextEpisode"), style = T.Eyebrow)
                Text("S${next.season} · E${next.epLabel()}", style = T.H2)
                next.name?.let { Text(it, style = T.Small.copy(color = C.Text2), maxLines = 1) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("play" to t("playNow"), "cred" to t("credits")).forEach { (k, l) ->
                        val on = active && upSel == k
                        Text(l, style = T.Btn.copy(color = if (on) C.Bg else C.Text1), modifier = Modifier.background(if (on) C.Text1 else C.Surface2, R6).padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
                if (!active) Text("▲", style = T.Small.copy(color = C.Text3), modifier = Modifier.padding(top = 6.dp))
            }
        }

        // audio & subtitles sheet
        AnimatedVisibility(menu, enter = slideInHorizontally { it } + fadeIn(), exit = slideOutHorizontally { it } + fadeOut(), modifier = Modifier.align(Alignment.CenterEnd)) {
            val first = remember { FocusRequester() }
            Column(Modifier.fillMaxHeight().width(320.dp).background(C.Bg.copy(alpha = .96f)).padding(28.dp)) {
                var firstUsed = false
                fun mod(): Modifier = if (!firstUsed) { firstUsed = true; Modifier.focusRequester(first) } else Modifier
                if (audio.isNotEmpty()) {
                    Text(t("audio"), style = T.Eyebrow); Spacer(Modifier.height(8.dp))
                    audio.forEach { a -> MenuOpt(a.label, a.selected, mod()) {
                        val g = player.currentTracks.groups.filter { it.type == MC.TRACK_TYPE_AUDIO }.getOrNull(a.index) ?: return@MenuOpt
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                        menu = false
                    } }
                    Spacer(Modifier.height(18.dp))
                }
                Text(t("subtitles"), style = T.Eyebrow); Spacer(Modifier.height(8.dp))
                (listOf("off") + listOf("fa", "en").filter { item.subs.containsKey(it) }).forEach { l ->
                    MenuOpt(when (l) { "off" -> t("off"); "fa" -> t("persian"); else -> t("english") }, subLang == l, mod()) {
                        Prefs.defaultSub = l; setSub(l); menu = false
                    }
                }
            }
            LaunchedEffect(Unit) { delay(50); runCatching { first.requestFocus() } }
        }

        // episodes sheet (current season)
        AnimatedVisibility(eps, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            val season = item.season
            val list = items.withIndex().filter { it.value.season == season }
            val curFr = remember { FocusRequester() }
            val ls = rememberLazyListState(initialFirstVisibleItemIndex = list.indexOfFirst { it.index == index }.coerceAtLeast(0))
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .92f)))).padding(top = 40.dp, bottom = 30.dp)) {
                Text("${t("season")} $season  ·  ${list.size} ${t("episodes")}", style = T.H2, modifier = Modifier.padding(start = 48.dp, bottom = 12.dp))
                LazyRow(state = ls, contentPadding = PaddingValues(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list, key = { it.index }) { (i, ep) ->
                        EpisodeTile(ep, i, 0f, current = i == index, modifier = if (i == index) Modifier.focusRequester(curFr) else Modifier) { eps = false; playIndex(i) }
                    }
                }
            }
            LaunchedEffect(Unit) { delay(60); runCatching { curFr.requestFocus() } }
        }
    }
    // keep key focus on the player whenever no sheet is open
    LaunchedEffect(menu, eps) { if (!menu && !eps) { delay(30); runCatching { rootFr.requestFocus() } } }
}

@Composable
private fun MenuOpt(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Btn(label, if (selected) Icons.Default.Check else null, modifier = modifier.fillMaxWidth().padding(vertical = 2.dp), onClick = onClick)
}
