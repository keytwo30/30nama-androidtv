package com.nama30.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nama30.tv.data.Api
import com.nama30.tv.data.Home
import com.nama30.tv.data.Post
import com.nama30.tv.data.Prefs
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private data class Row0(val key: String, val title: String, val posts: List<Post>)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen() {
    var home by remember { mutableStateOf<Home?>(null) }
    var cw by remember { mutableStateOf<List<Post>>(emptyList()) }
    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = false
        try {
            coroutineScope {
                val c = async { if (Prefs.loggedIn) Api.continueWatching() else emptyList() }
                home = Api.home(); cw = c.await()
            }
        } catch (e: Exception) { error = true }
    }
    val h = home
    when {
        error -> ErrorView { attempt++ }
        h == null -> Loading()
        else -> HomeContent(h, cw)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HomeContent(h: Home, cw: List<Post>) {
    val lang = Prefs.lang
    val rows = remember(h, cw, lang) {
        buildList {
            if (cw.isNotEmpty()) add(Row0("cw", t("continueWatching"), cw))
            h.rows.forEach { (k, ps) -> add(Row0(k, t(k), ps)) }
        }
    }
    val cwIds = remember(cw) { cw.map { it.id }.toSet() }
    var heroIdx by rememberSaveable { mutableIntStateOf(0) }
    var browse by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf<Pair<Post, String>?>(null) }  // post + eyebrow while browsing
    var lastKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val fm = rememberFocusMem()
    val playFr = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val hero = h.heroes.getOrNull(heroIdx)
    val shown: Post? = if (browse) focused?.first else hero
    val eyebrow = if (browse) focused?.second ?: "" else hero?.let { "${t("featured")} · ${typeLabel(it, true)}" } ?: ""

    // hero auto-advance while idle on the billboard
    LaunchedEffect(browse, heroIdx, lastKey) {
        if (browse || h.heroes.size < 2) return@LaunchedEffect
        delay(9000); heroIdx = (heroIdx + 1) % h.heroes.size
    }
    // backdrop follows focus, debounced so fast scrolling doesn't decode every cover
    var backdrop by remember { mutableStateOf(shown?.cover) }
    LaunchedEffect(shown?.id) { delay(if (backdrop == null) 0 else 220); backdrop = shown?.cover ?: shown?.posterLarge }

    val billboardH by animateDpAsState(if (browse) 200.dp else 310.dp, label = "bb")

    Box(Modifier.fillMaxSize().onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) lastKey = System.currentTimeMillis(); false }) {
        Backdrop(backdrop)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(billboardH)) {
                Column(Modifier.align(Alignment.BottomStart).padding(start = START_PAD.dp, bottom = 12.dp).widthIn(max = 520.dp)) {
                    if (shown != null) Billboard(shown, eyebrow, compact = browse)
                    AnimatedVisibility(!browse && hero != null, enter = fadeIn(), exit = fadeOut()) {
                        Column {
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.onFocusChanged { if (it.hasFocus) browse = false }) {
                                Btn(if (hero != null && hero.id in cwIds) t("resume") else t("play"), Icons.Default.PlayArrow, primary = true,
                                    modifier = Modifier.focusRequester(playFr).memFocus(fm, "play")) { hero?.let { Nav.go(Screen.Details(it.id, autoplay = true)) } }
                                Btn(t("details"), Icons.Outlined.Info, modifier = Modifier.onPreviewKeyEvent {
                                    // Right past the last button flips to the next featured title
                                    val toEnd = if (rtl) Key.DirectionLeft else Key.DirectionRight
                                    if (it.type == KeyEventType.KeyDown && it.key == toEnd && h.heroes.size > 1) { heroIdx = (heroIdx + 1) % h.heroes.size; true } else false
                                }) { hero?.let { Nav.go(Screen.Details(it.id)) } }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                h.heroes.indices.forEach { i -> Box(Modifier.dot().background(if (i == heroIdx) C.Accent else C.Text3.copy(alpha = .6f))) }
                            }
                        }
                    }
                }
            }
            LazyColumn(
                state = listState, modifier = Modifier.fillMaxSize().onFocusChanged { if (it.hasFocus) browse = true },
                contentPadding = PaddingValues(bottom = 200.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                itemsIndexed(rows, key = { _, r -> r.key }) { ri, r ->
                    Column(Modifier.onPreviewKeyEvent {
                        // Up from the first row returns to the billboard buttons (hidden while browsing)
                        if (ri == 0 && hero != null && it.type == KeyEventType.KeyDown && it.key == Key.DirectionUp) {
                            browse = false; scope.launch { repeat(5) { delay(32); if (runCatching { playFr.requestFocus() }.isSuccess) return@launch } }; true
                        } else false
                    }) {
                        Text(r.title, style = T.H2, modifier = Modifier.padding(start = START_PAD.dp, bottom = 8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(start = START_PAD.dp, end = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.focusRestorer().focusGroup(),
                        ) {
                            itemsIndexed(r.posts, key = { _, p -> p.id }) { i, p ->
                                val k = "${r.key}:${p.id}"
                                val onFocus = { focused = p to when (r.key) { "top_10" -> "${t("top10")} · #${i + 1}"; "cw" -> t("continueWatching"); else -> typeLabel(p, true) } }
                                val m = Modifier.memFocus(fm, k)
                                when (r.key) {
                                    "cw" -> LandCard(p, cwLabel(p), (p.cw?.progress ?: 0.0).toFloat() / 100f, m, onFocus) { Nav.go(Screen.Details(p.id)) }
                                    "top_10" -> Row(verticalAlignment = Alignment.Bottom) {
                                        Text("${i + 1}", style = T.Hero.copy(fontSize = 64.sp, color = C.Surface2, fontWeight = FontWeight.ExtraBold), modifier = Modifier.padding(end = 2.dp))
                                        PosterCard(p, modifier = m, onFocus = onFocus) { Nav.go(Screen.Details(p.id)) }
                                    }
                                    else -> PosterCard(p, modifier = m, onFocus = onFocus) { Nav.go(Screen.Details(p.id)) }
                                }
                            }
                        }
                    }
                }
                if (!Prefs.loggedIn) item {
                    Column(Modifier.padding(start = START_PAD.dp, top = 8.dp)) {
                        Text(t("loginRequired"), style = T.Body.copy(color = C.Text2)); Spacer(Modifier.height(10.dp))
                        Btn(t("signIn")) { Nav.reset(Screen.Login) }
                    }
                }
            }
        }
        Clock(Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 40.dp))
    }
    if (fm.pending == null) AutoFocus(playFr)
}

private fun cwLabel(p: Post): String {
    val c = p.cw
    return if (c?.season != null && c.episode != null) "S${c.season} · E${c.episode} · ${p.title(Prefs.lang)}" else p.title(Prefs.lang)
}

@Composable
fun Billboard(p: Post, eyebrow: String, compact: Boolean) {
    val lang = Prefs.lang
    Column {
        Text(eyebrow, style = T.Eyebrow)
        Spacer(Modifier.height(4.dp))
        Text(p.title(lang), style = if (compact) T.H1 else T.Hero, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        MetaLine(p)
        Spacer(Modifier.height(8.dp))
        Text(p.plot(lang), style = T.Body.copy(color = C.Text2, lineHeight = 20.sp), maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis)
    }
}

/** Cover on the end side, faded into the background on its start and bottom edges. */
@Composable
fun Backdrop(url: String?, alpha: Float = 1f) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(.8f).fillMaxHeight(.85f)) {
            Crossfade(url, label = "bd", modifier = Modifier.fillMaxSize()) { u ->
                if (u != null) AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(u).size(1280, 720).crossfade(false).build(),
                    contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(), alpha = alpha,
                )
            }
            val fade = listOf(C.Bg, C.Bg.copy(alpha = .82f), C.Bg.copy(alpha = .35f), Color.Transparent)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(if (rtl) fade.reversed() else fade)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.45f to Color.Transparent, 1f to C.Bg)))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.55f to Color.Transparent, .9f to C.Bg.copy(alpha = .7f))))
    }
}

@Composable
fun Clock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val c = Calendar.getInstance(); now = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            delay(20_000)
        }
    }
    Text(now, style = T.Body.copy(color = C.Text2), modifier = modifier)
}
