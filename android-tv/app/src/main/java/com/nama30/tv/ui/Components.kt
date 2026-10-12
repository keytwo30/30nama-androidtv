package com.nama30.tv.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nama30.tv.data.Post
import kotlinx.coroutines.delay

val R6 = RoundedCornerShape(6.dp)
val R10 = RoundedCornerShape(10.dp)
const val START_PAD = 92   // content start inset (rail is 56dp)

@Composable
fun Img(url: String?, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Crop, crossfade: Boolean = true) {
    if (url.isNullOrBlank()) { Box(modifier); return }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(crossfade).build(),
        contentDescription = null, contentScale = scale, modifier = modifier,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Btn(text: String, icon: ImageVector? = null, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(36.dp),
        shape = ButtonDefaults.shape(R6),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) C.Text1.copy(alpha = .92f) else C.Surface2.copy(alpha = .85f),
            contentColor = if (primary) C.Bg else C.Text1,
            focusedContainerColor = C.Text1, focusedContentColor = C.Bg,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = T.Btn.copy(color = Color.Unspecified))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Pill(text: String, selected: Boolean = false, modifier: Modifier = Modifier, onFocus: (() -> Unit)? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.height(30.dp).onFocusChanged { if (it.isFocused) onFocus?.invoke() },
        shape = ButtonDefaults.shape(RoundedCornerShape(50)),
        colors = ButtonDefaults.colors(
            containerColor = if (selected) C.Text1.copy(alpha = .9f) else C.Surface2.copy(alpha = .8f),
            contentColor = if (selected) C.Bg else C.Text2,
            focusedContainerColor = C.Text1, focusedContentColor = C.Bg,
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.06f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
    ) { Text(text, style = T.Btn.copy(color = Color.Unspecified)) }
}

/** Poster card (2:3). Badge for free / coming soon. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(p: Post, width: Dp = 100.dp, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.width(width).height(width * 1.5f).onFocusChanged { if (it.isFocused) onFocus() },
        shape = CardDefaults.shape(R6),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, C.Text1), shape = R6)),
        colors = CardDefaults.colors(containerColor = C.Surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(p.title(com.nama30.tv.data.Prefs.lang), style = T.Small.copy(color = C.Text3), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(8.dp), maxLines = 3)
            Img(p.poster, Modifier.fillMaxSize())
            val badge = when { p.comingSoon -> t("soon"); p.free -> t("free"); else -> null }
            if (badge != null) Text(badge, style = T.Small.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), modifier = Modifier
                .align(Alignment.TopStart).padding(5.dp).background(if (p.free) C.Green else C.Accent, R6).padding(horizontal = 5.dp, vertical = 1.dp))
        }
    }
}

/** 16:9 card with label + progress (continue watching). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LandCard(p: Post, label: String, progress: Float, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.width(200.dp).height(112.dp).onFocusChanged { if (it.isFocused) onFocus() },
        shape = CardDefaults.shape(R6),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        border = CardDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, C.Text1), shape = R6)),
        colors = CardDefaults.colors(containerColor = C.Surface),
    ) {
        Box(Modifier.fillMaxSize()) {
            Img(p.cover ?: p.poster, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = .85f))))
            Text(label, style = T.Small.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 10.dp))
            if (progress > 0f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .25f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(C.Accent))
            }
        }
    }
}

/** IMDb · 30nama score · year · duration · age · genres */
@Composable
fun MetaLine(p: Post, genres: Int = 2) {
    val lang = com.nama30.tv.data.Prefs.lang
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        p.imdb?.takeIf { it > 0 }?.let { Text("IMDb ${fmtScore(it)}", style = T.Small.copy(fontWeight = FontWeight.Bold), modifier = Modifier.border(1.dp, C.Text2, R6).padding(horizontal = 5.dp, vertical = 1.dp)) }
        p.score?.takeIf { it > 0 }?.let { Text(fmtScore(it), style = T.Body.copy(color = C.Green, fontWeight = FontWeight.Bold)) }
        val bits = buildList {
            p.year?.let { add(if (p.isSeries && p.yearEnd != null && p.yearEnd != it) "$it–${p.yearEnd}" else "$it") }
            p.minutes?.takeIf { it > 0 }?.let { m -> val h = m / 60; val mm = m % 60
                add(if (lang == "fa") listOfNotNull(if (h > 0) "$h ساعت" else null, if (mm > 0) "$mm دقیقه" else null).joinToString(" ")
                    else listOfNotNull(if (h > 0) "${h}h" else null, if (mm > 0) "${mm}m" else null).joinToString(" ")) }
        }
        if (bits.isNotEmpty()) Text(bits.joinToString("  ·  "), style = T.Body.copy(color = C.Text2))
        p.age?.let { Text(it, style = T.Small.copy(color = C.Text2), modifier = Modifier.border(1.dp, C.Text3, R6).padding(horizontal = 5.dp, vertical = 1.dp)) }
        if (p.genres.isNotEmpty()) Text(p.genres.take(genres).joinToString(", "), style = T.Body.copy(color = C.Text2), maxLines = 1)
    }
}

fun fmtScore(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

fun typeLabel(p: Post, withSeasons: Boolean): String {
    var s = when { p.type == "anime" -> t("anime"); p.isSeries -> t("series"); else -> t("movie") }
    if (withSeasons && p.isSeries && p.seasons != null) s += " · ${p.seasons} ${t("seasons")}"
    return s
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Spinner()
    }
}

@Composable
fun ErrorView(onRetry: () -> Unit) {
    val fr = remember { FocusRequester() }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Warning, null, tint = C.Accent, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(12.dp))
        Text("${t("error")}. ${t("tryAgain")}", style = T.Body.copy(color = C.Text2))
        Spacer(Modifier.height(16.dp))
        Btn(t("retry"), primary = true, modifier = Modifier.focusRequester(fr), onClick = onRetry)
    }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
}

/** Requests focus once the composable is laid out (retries briefly while lazy lists attach). */
@Composable
fun AutoFocus(fr: FocusRequester, key: Any? = Unit) {
    LaunchedEffect(key) {
        repeat(10) { if (runCatching { fr.requestFocus() }.isSuccess) return@LaunchedEffect; delay(50) }
    }
}

@Composable
fun Toast() {
    val tt = Nav.toast ?: return
    var visible by remember(tt) { mutableStateOf(true) }
    LaunchedEffect(tt) { delay(3200); visible = false; if (Nav.toast == tt) Nav.toast = null }
    if (!visible) return
    Box(Modifier.fillMaxSize().padding(bottom = 36.dp), contentAlignment = Alignment.BottomCenter) {
        Text(tt.first, style = T.Body, modifier = Modifier.clip(R10).background(C.Surface2).padding(horizontal = 18.dp, vertical = 10.dp))
    }
}

fun Modifier.dot() = this.size(6.dp).clip(CircleShape)

@Composable
fun Spinner(size: Dp = 36.dp) {
    val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val a by tr.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)), label = "a")
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        drawArc(C.Accent, a, 100f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

/** Remembers which item had focus so Back returns to it (survives via SaveableStateProvider). */
class FocusMem(saved: String?) {
    var key: String? = saved
    var pending: String? = saved
}

@Composable
fun rememberFocusMem(): FocusMem {
    var saved by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val fm = remember { FocusMem(saved) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { saved = fm.key } }
    return fm
}

@Composable
fun Modifier.memFocus(fm: FocusMem, k: String): Modifier {
    val fr = remember { FocusRequester() }
    if (fm.pending == k) LaunchedEffect(Unit) {
        repeat(10) { if (runCatching { fr.requestFocus() }.isSuccess) { fm.pending = null; return@LaunchedEffect }; delay(40) }
        fm.pending = null
    }
    return this.focusRequester(fr).onFocusChanged { if (it.isFocused) fm.key = k }
}
