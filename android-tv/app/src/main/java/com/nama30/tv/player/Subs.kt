package com.nama30.tv.player

class Cue(val start: Double, val end: Double, val text: String)

/** SRT / WebVTT -> cues sorted by start. Tags and ASS positioning codes are stripped. */
private val TS = Regex("""^(?:(\d+):)?(\d+):(\d+)[.,](\d+)$""")
private val BLOCKS = Regex("\n\n+")
private val SPACES = Regex("\\s+")
private val TAGS = Regex("<[^>]+>")
private val ASS = Regex("""\{\\an?\d\}""")

fun parseSubs(src: String): List<Cue> {
    val ts = TS
    fun t(s: String): Double? {
        val m = ts.find(s.trim()) ?: return null
        val (h, mi, se, ms) = m.destructured
        return (h.toIntOrNull() ?: 0) * 3600 + mi.toInt() * 60 + se.toInt() + "0.$ms".toDouble()
    }
    val out = ArrayList<Cue>()
    for (b in src.replace("\r", "").removePrefix("﻿").split(BLOCKS)) {
        val lines = b.split('\n')
        val i = lines.indexOfFirst { "-->" in it }
        if (i < 0) continue
        val parts = lines[i].split("-->")
        val s = t(parts[0]) ?: continue
        val e = t(parts[1].trim().split(SPACES)[0]) ?: continue
        val txt = lines.drop(i + 1).joinToString("\n")
            .replace(TAGS, "").replace(ASS, "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").trim()
        if (txt.isNotEmpty()) out += Cue(s, e, txt)
    }
    out.sortBy { it.start }
    return out
}

/** Text active at time t (binary search, then look back a few cues for overlaps). */
fun cueAt(cues: List<Cue>, t: Double): String {
    var lo = 0; var hi = cues.size - 1; var idx = -1
    while (lo <= hi) { val mid = (lo + hi) ushr 1; if (cues[mid].start <= t) { idx = mid; lo = mid + 1 } else hi = mid - 1 }
    var j = idx
    while (j >= 0 && j > idx - 4) { if (cues[j].end > t) return cues[j].text; j-- }
    return ""
}
