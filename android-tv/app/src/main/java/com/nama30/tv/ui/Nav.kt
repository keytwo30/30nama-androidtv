package com.nama30.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nama30.tv.data.Post
import com.nama30.tv.data.Resume
import com.nama30.tv.data.StreamItem

sealed interface Screen {
    data object Login : Screen
    data object Home : Screen
    data class Details(val id: Long, val autoplay: Boolean = false) : Screen
    data object Search : Screen
    data object Browse : Screen
    data class Archive(val slug: String, val name: String) : Screen
    data class Settings(val section: String = "account") : Screen
}

data class PlayRequest(val post: Post, val items: List<StreamItem>, val index: Int, val resume: Resume?)

object Nav {
    val stack = mutableStateListOf<Screen>()
    var player by mutableStateOf<PlayRequest?>(null)
    var toast by mutableStateOf<Pair<String, Long>?>(null)

    val top: Screen? get() = stack.lastOrNull()

    fun go(s: Screen) { stack.add(s) }
    fun reset(s: Screen) { stack.clear(); stack.add(s) }
    fun replace(s: Screen) { if (stack.isNotEmpty()) stack[stack.lastIndex] = s else stack.add(s) }
    /** false when there is nothing to pop. */
    fun back(): Boolean { if (stack.size <= 1) return false; stack.removeAt(stack.lastIndex); return true }

    fun play(r: PlayRequest) { player = r }
    fun showToast(msg: String) { toast = msg to System.currentTimeMillis() }

    /** Which rail entry is "current": the nearest non-details screen. */
    fun railKey(): String = when (val s = stack.lastOrNull { it !is Screen.Details }) {
        Screen.Home -> "home"; Screen.Search -> "search"; Screen.Browse -> "archive"
        is Screen.Archive -> if (s.slug in setOf("movies", "series", "anime")) s.slug else "archive"
        is Screen.Settings -> if (s.section == "account") "account" else "settings"
        else -> ""
    }
}
