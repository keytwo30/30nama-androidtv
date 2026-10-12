package com.nama30.tv.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nama30.tv.data.Api
import com.nama30.tv.data.Post
import com.nama30.tv.data.Prefs
import com.nama30.tv.data.Stream
import com.nama30.tv.data.StreamItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(id: Long, autoplay: Boolean) {
    var post by remember { mutableStateOf(Api.cachedSingle(id)) }
    var stream by remember { mutableStateOf(Api.cachedStream(id)) }
    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = false
        try {
            // cached (Back / player closed): show it now, refresh the resume point quietly
            if (post != null && stream != null && !autoplay) {
                Api.stream(id, free = !Prefs.loggedIn)?.let { stream = it }
                return@LaunchedEffect
            }
            val p = Api.single(id)
            // set both at once: autoplay must see the stream
            stream = if (Prefs.loggedIn || p.free) Api.stream(id, free = !Prefs.loggedIn) else null
            post = p
        } catch (e: Exception) { error = true }
    }
    val p = post
    when {
        error -> ErrorView { attempt++ }
        p == null -> Loading()
        else -> DetailsContent(p, stream, autoplay)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DetailsContent(p: Post, st: Stream?, autoplay: Boolean) {
    val lang = Prefs.lang
    val items = st?.items.orEmpty()
    val resume = st?.resume
    val startIdx = remember(st) { resume?.let { r -> items.indexOfFirst { it.fileId == r.fileId }.takeIf { it >= 0 } } ?: 0 }
    val seasons = remember(items) { items.map { it.season }.distinct() }
    var season by rememberSaveable { mutableIntStateOf(items.getOrNull(startIdx)?.season ?: 1) }
    var autoplayed by rememberSaveable { mutableStateOf(false) }
    val playFr = remember { FocusRequester() }
    val epFr = remember { FocusRequester() }
    val fm = rememberFocusMem()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun play(i: Int) {
        if (items.isEmpty()) { Nav.showToast(if (Prefs.loggedIn) t("noStream") else t("playLogin")); return }
        Nav.play(com.nama30.tv.ui.PlayRequest(p, items, i, resume))
    }
    LaunchedEffect(Unit) { if (autoplay && !autoplayed) { autoplayed = true; play(startIdx) } }

    Box(Modifier.fillMaxSize()) {
        Backdrop(p.cover ?: p.posterLarge)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 70.dp, bottom = 80.dp)) {
            item {
                Column(Modifier.padding(start = START_PAD.dp).widthIn(max = 560.dp)) {
                    Row {
                        Text(typeLabel(p, true), style = T.Eyebrow)
                        if (p.persianSub) Text("  ·  ${t("persianSub")}", style = T.Eyebrow.copy(color = C.Text2))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(p.title(lang), style = T.Hero, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    p.altTitle(lang)?.let { Text(it, style = T.Body.copy(color = C.Text2)) }
                    Spacer(Modifier.height(10.dp))
                    MetaLine(p, genres = 3)
                    Spacer(Modifier.height(10.dp))
                    Text(p.plot(lang), style = T.Body.copy(color = C.Text2, lineHeight = 20.sp), maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val cur = items.getOrNull(startIdx)
                        val label = if (resume != null) t("resume") + (if (p.isSeries && cur != null) " S${cur.season}·E${cur.epLabel()}" else "") else t("play")
                        Btn(label, Icons.Default.PlayArrow, primary = true, modifier = Modifier.focusRequester(playFr).memFocus(fm, "play")) { play(startIdx) }
                        if (p.isSeries && items.isNotEmpty()) Btn(t("episodes"), Icons.AutoMirrored.Filled.List, modifier = Modifier.memFocus(fm, "eps")) {
                            scope.launch { listState.animateScrollToItem(1); runCatching { epFr.requestFocus() } }
                        }
                    }
                    if (p.cast.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("${t("cast")}: ${p.cast.take(6).joinToString(", ")}", style = T.Small.copy(color = C.Text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (p.isSeries && items.isNotEmpty()) {
                item {
                    Column(Modifier.padding(top = 36.dp)) {
                        if (seasons.size > 1) LazyRow(
                            contentPadding = PaddingValues(start = START_PAD.dp, end = 48.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.focusRestorer().focusGroup(),
                        ) {
                            items(seasons) { s -> Pill("${t("season")} $s", selected = s == season, modifier = Modifier.memFocus(fm, "s$s"), onFocus = { season = s }) { season = s } }
                        } else Text("${t("season")} ${seasons.first()}", style = T.H2, modifier = Modifier.padding(start = START_PAD.dp))
                        Spacer(Modifier.height(14.dp))
                        val eps = items.withIndex().filter { it.value.season == season }
                        LazyRow(
                            contentPadding = PaddingValues(start = START_PAD.dp, end = 48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.focusRequester(epFr).focusRestorer().focusGroup(),
                        ) {
                            items(eps, key = { it.value.fileId + it.index }) { (i, it) ->
                                val prog = if (resume != null && resume.fileId == it.fileId && i == startIdx) -1f else 0f
                                EpisodeTile(it, i, prog, current = resume?.fileId == it.fileId, modifier = Modifier.memFocus(fm, "e${it.fileId}")) { play(i) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (fm.pending == null) AutoFocus(playFr)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeTile(it: StreamItem, idx: Int, progress: Float, current: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val n = it.epLabel()
    Card(
        onClick = onClick, modifier = modifier.width(176.dp).height(92.dp),
        shape = CardDefaults.shape(R6), scale = CardDefaults.scale(focusedScale = 1.06f),
        border = CardDefaults.border(focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, C.Text1), shape = R6)),
        colors = CardDefaults.colors(containerColor = C.Surface.copy(alpha = .9f), focusedContainerColor = C.Surface2),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(12.dp)) {
                Text("E" + (if (n.length == 1) "0$n" else n), style = T.H1.copy(fontSize = 20.sp, color = if (current) C.Accent else C.Text1))
                Spacer(Modifier.height(2.dp))
                Text(it.name ?: "${t("episode")} $n", style = T.Small.copy(color = C.Text2), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (current) Box(Modifier.align(androidx.compose.ui.Alignment.BottomStart).fillMaxWidth().height(3.dp).background(C.Accent))
        }
    }
}
