package com.nama30.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.nama30.tv.data.Api
import com.nama30.tv.data.Post
import com.nama30.tv.data.Prefs
import kotlinx.coroutines.delay

private val KB = mapOf(
    "en" to (7 to "abcdefghijklmnopqrstuvwxyz0123456789".map { it.toString() }),
    "fa" to (8 to "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی۰۱۲۳۴۵۶۷۸۹".map { it.toString() }),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Key(label: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, w: Int = 34, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = modifier.width(w.dp).height(34.dp), shape = ButtonDefaults.shape(R6),
        colors = ButtonDefaults.colors(containerColor = C.Surface.copy(alpha = .8f), contentColor = C.Text1, focusedContainerColor = C.Text1, focusedContentColor = C.Bg),
        scale = ButtonDefaults.scale(focusedScale = 1.1f), contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, null, Modifier.size(16.dp))
            else Text(label ?: "", style = T.Btn.copy(color = androidx.compose.ui.graphics.Color.Unspecified, fontSize = if (label!!.length > 2) 11.sp else 15.sp), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen() {
    var q by rememberSaveable { mutableStateOf("") }
    var kb by rememberSaveable { mutableStateOf(if (Prefs.lang == "fa") "fa" else "en") }
    var results by remember { mutableStateOf<List<Post>>(emptyList()) }
    var header by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val firstKey = remember { FocusRequester() }
    val field = remember { FocusRequester() }
    val fm = rememberFocusMem()

    LaunchedEffect(q) {
        delay(if (q.isEmpty()) 0 else 500)
        val qq = q.trim()
        suspend fun popular(m: String) {
            val h = runCatching { Api.home() }.getOrNull() ?: return
            results = (h.rows.firstOrNull { it.first == "suggested" } ?: h.rows.firstOrNull())?.second.orEmpty().take(15)
            header = t("popularNow"); msg = m
        }
        if (qq.isEmpty()) { popular(""); return@LaunchedEffect }
        try {
            val r = Api.search(qq)
            if (r.isEmpty()) popular("${t("nothingFor")} “$qq”") else { results = r; header = t("topResults"); msg = "" }
        } catch (e: Exception) { popular("${t("error")}. ${t("tryAgain")}") }
    }

    Row(Modifier.fillMaxSize().background(C.Bg).padding(start = START_PAD.dp, top = 40.dp)) {
        Column(Modifier.width(300.dp)) {
            BasicTextField(
                value = q, onValueChange = { q = it.take(60) }, singleLine = true,
                textStyle = T.H2.copy(color = C.Text1), cursorBrush = SolidColor(C.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { runCatching { firstKey.requestFocus() } }),
                modifier = Modifier.fillMaxWidth().focusRequester(field).border(1.dp, C.Surface2, R6).background(C.Surface.copy(alpha = .7f), R6).padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner -> Box { if (q.isEmpty()) Text(t("searchPh"), style = T.Body.copy(color = C.Text3)); inner() } },
            )
            Spacer(Modifier.height(16.dp))
            val (cols, keys) = KB.getValue(kb)
            Column(Modifier.focusRestorer().focusGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.chunked(cols).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { c, k -> Key(k, modifier = if (r == 0 && c == 0) Modifier.focusRequester(firstKey) else Modifier) { if (q.length < 60) q += k } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Key(t("space"), w = 74) { q += " " }
                    Key(icon = Icons.AutoMirrored.Outlined.Backspace) { q = q.dropLast(1) }
                    Key(t("clearK"), w = 50) { q = "" }
                    Key(if (kb == "en") "فا" else "EN", w = 40) { kb = if (kb == "en") "fa" else "en" }
                    Key(icon = Icons.Outlined.Keyboard) { runCatching { field.requestFocus() } }
                }
            }
        }
        Spacer(Modifier.width(32.dp))
        Column(Modifier.fillMaxSize()) {
            Text(header, style = T.H2)
            if (msg.isNotEmpty()) Text(msg, style = T.Body.copy(color = C.Text2), modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            PosterGrid(results, cols = 5, fm = fm)
        }
    }
    if (fm.pending == null) AutoFocus(firstKey)
}

/** Poster grid with titles; onNearEnd lets callers page. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PosterGrid(posts: List<Post>, cols: Int, fm: FocusMem, state: androidx.compose.foundation.lazy.grid.LazyGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(),
               first: FocusRequester? = null, onNearEnd: () -> Unit = {}) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(cols), state = state, modifier = Modifier.fillMaxSize().focusRestorer().focusGroup(),
        contentPadding = PaddingValues(top = 6.dp, end = 40.dp, bottom = 60.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(posts.size, key = { posts[it].id }) { i ->
            val p = posts[i]
            Column {
                var m = Modifier.memFocus(fm, "g${p.id}")
                if (i == 0 && first != null) m = m.focusRequester(first)
                PosterCard(p, width = 100.dp, modifier = m, onFocus = { if (i >= posts.size - cols * 2) onNearEnd() }) { Nav.go(Screen.Details(p.id)) }
                Text(p.title(Prefs.lang), style = T.Small.copy(color = C.Text2), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(100.dp).padding(top = 5.dp))
            }
        }
    }
}
