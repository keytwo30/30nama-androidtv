package com.nama30.tv.data

import org.json.JSONArray
import org.json.JSONObject

/* Lenient accessors: the API mixes types (episode "11" vs 11, times "00:01:05" vs seconds, null vs missing). */

fun JSONObject?.obj(k: String): JSONObject? = this?.optJSONObject(k)
fun JSONObject?.arr(k: String): JSONArray? = this?.optJSONArray(k)
fun JSONObject?.str(k: String): String? {
    if (this == null || isNull(k)) return null
    val v = opt(k) ?: return null
    return if (v is String) v.ifBlank { null } else if (v is Number || v is Boolean) v.toString() else null
}
fun JSONObject?.num(k: String): Double? {
    if (this == null || isNull(k)) return null
    return when (val v = opt(k)) { is Number -> v.toDouble(); is String -> v.trim().toDoubleOrNull(); else -> null }
}
fun JSONObject?.int(k: String): Int? = num(k)?.toInt()
fun JSONObject?.bool(k: String): Boolean = this?.optBoolean(k, false) ?: false

inline fun <T> JSONArray?.mapObj(f: (JSONObject) -> T?): List<T> {
    if (this == null) return emptyList()
    val out = ArrayList<T>(length())
    for (i in 0 until length()) optJSONObject(i)?.let { f(it)?.let(out::add) }
    return out
}

/** "01:02:03" | "62:03" | "3723" -> seconds */
fun parseClock(v: String?): Double {
    if (v.isNullOrBlank()) return 0.0
    if (!v.contains(':')) return v.toDoubleOrNull() ?: 0.0
    return v.split(':').fold(0.0) { acc, p -> acc * 60 + (p.toDoubleOrNull() ?: 0.0) }
}
