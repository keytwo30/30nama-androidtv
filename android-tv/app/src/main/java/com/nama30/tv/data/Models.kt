package com.nama30.tv.data

import org.json.JSONObject

data class User(val id: String, val name: String, val email: String?, val type: String?, val expire: String?, val expireDays: Int?) {
    companion object {
        fun from(o: JSONObject) = User(
            id = o.str("user_id") ?: o.str("id") ?: "",
            name = o.str("user_name") ?: o.str("user_email") ?: "user",
            email = o.str("user_email"),
            type = o.str("user_type"),
            expire = o.obj("user_expire")?.let { it.str("g")?.take(10) } ?: o.str("user_expire"),
            expireDays = o.obj("user_expire").int("days"),
        )
    }
}

data class Post(
    val id: Long,
    val titleEn: String?,
    val titleLocal: String?,
    val plotEn: String?,
    val plotLocal: String?,
    val type: String,          // movie | series | anime
    val isSeries: Boolean,
    val seasons: Int?,
    val year: Int?,
    val yearEnd: Int?,
    val age: String?,
    val minutes: Int?,
    val imdb: Double?,
    val score: Double?,
    val genres: List<String>,
    val poster: String?,       // card-sized poster
    val posterLarge: String?,
    val cover: String?,        // 16:9 backdrop
    val cast: List<String>,
    val free: Boolean,
    val comingSoon: Boolean,
    val persianSub: Boolean,
    val cw: CwInfo?,
) {
    fun title(lang: String) = (if (lang == "fa") titleLocal ?: titleEn else titleEn ?: titleLocal) ?: ""
    fun altTitle(lang: String): String? { val o = if (lang == "fa") titleEn else titleLocal; return o?.takeIf { it != title(lang) } }
    fun plot(lang: String) = (if (lang == "fa") plotLocal ?: plotEn else plotEn ?: plotLocal) ?: ""

    companion object {
        fun from(o0: JSONObject): Post? {
            val o = o0.obj("post") ?: o0
            val id = o.num("id")?.toLong() ?: return null
            val opt = o.obj("options"); val info = o.obj("info"); val img = o.obj("image")
            val poster = img.obj("poster")
            fun pick(vararg sizes: String): String? {
                for (fmt in arrayOf("webp", "jpg")) for (s in sizes) poster.obj(fmt).str(s)?.let { return it }
                return null
            }
            val cover = img.obj("cover").let { it.str("webp") ?: it.str("jpg") }
            val sc = o.obj("score")
            val d = o.obj("data")
            return Post(
                id = id,
                titleEn = o.obj("title").str("english"), titleLocal = o.obj("title").str("local"),
                plotEn = o.obj("plot").str("english"), plotLocal = o.obj("plot").str("local"),
                type = opt.str("title_type") ?: if (opt.bool("is_series")) "series" else "movie",
                isSeries = opt.bool("is_series"),
                seasons = info.int("seasons"), year = info.int("year"), yearEnd = info.int("year_end"),
                age = info.str("age"), minutes = info.obj("time").int("default"),
                imdb = sc.obj("imdb").num("score"), score = sc.obj("30nama").num("score"),
                genres = o.arr("genre").mapObj { it.str("name") },
                poster = pick("medium", "large", "small"), posterLarge = pick("large", "big", "medium"),
                cover = cover,
                cast = o.arr("cast").mapObj { it.str("name") },
                free = opt.bool("free_stream"), comingSoon = opt.bool("coming_soon"), persianSub = opt.bool("persian_subtitle"),
                cw = d?.let { CwInfo(it.int("season"), it.str("episode"), it.num("progress") ?: 0.0) },
            )
        }
    }
}

/** Continue-watching extras on a post. progress is 0..100. */
data class CwInfo(val season: Int?, val episode: String?, val progress: Double)

data class Quality(val label: String, val url: String)

data class StreamItem(
    val fileId: String,
    val season: Int,
    val episode: String,
    val episodeNum: Int,
    val name: String?,
    val qualities: List<Quality>,
    val subs: Map<String, String>,
    val introStart: Double, val introEnd: Double,
    val recapStart: Double, val recapEnd: Double,
    val creditsAt: Double,
    val token: String,
) {
    /** Highest fixed rendition (e.g. 2160p > 1080p); "Auto" only when nothing else exists. */
    val bestUrl: String? get() {
        val fixed = qualities.filterNot { it.label.equals("auto", true) || it.label == "خودکار" }
        return (fixed.maxByOrNull { it.label.filter(Char::isDigit).toIntOrNull() ?: 0 } ?: qualities.firstOrNull())?.url
    }
    fun epLabel() = episode.replace(Regex("\\s*[,&+\\-–]\\s*"), "–")

    companion object {
        fun from(o: JSONObject): StreamItem? {
            val info = o.obj("info"); val file = o.obj("file"); val opt = o.obj("options")
            val m = file?.opt("m3u8")
            val qs = ArrayList<Quality>()
            when (m) {
                is String -> qs += Quality("Auto", m)
                is org.json.JSONArray -> for (i in 0 until m.length()) {
                    val e = m.opt(i)
                    if (e is String) qs += Quality("Auto", e)
                    else if (e is JSONObject) (e.str("default") ?: e.str("file") ?: e.str("url"))?.let { qs += Quality(e.str("label") ?: "Auto", it.trim()) }
                }
                is JSONObject -> (m.str("default") ?: m.str("file"))?.let { qs += Quality(m.str("label") ?: "Auto", it.trim()) }
            }
            if (qs.isEmpty()) return null
            val subs = HashMap<String, String>()
            file.obj("subtitle")?.let { s -> for (l in arrayOf("fa", "en")) s.str(l)?.let { subs[l] = it } }
            val ep = info.str("episode") ?: info.str("episode_number") ?: "1"
            return StreamItem(
                fileId = info.str("file_id") ?: "", season = info.int("season") ?: 1, episode = ep,
                episodeNum = ep.takeWhile(Char::isDigit).toIntOrNull() ?: info.int("episode_number") ?: 0,
                name = info.str("episode_name")?.takeUnless { Regex("^Season \\d+ - Episode \\d+$").matches(it) },
                qualities = qs, subs = subs,
                introStart = parseClock(opt.str("intro_start")), introEnd = parseClock(opt.str("intro_end")),
                recapStart = parseClock(opt.str("previously_start")), recapEnd = parseClock(opt.str("previously_end")),
                creditsAt = parseClock(opt.str("end")), token = opt.str("token") ?: "",
            )
        }
    }
}

data class Resume(val fileId: String, val time: Double)

data class Stream(val items: List<StreamItem>, val resume: Resume?)

data class Genre(val name: String, val slug: String, val count: Int?, val image: String?)
data class Menu(val categories: List<Genre>, val genres: List<Genre>)

data class Page(val posts: List<Post>, val page: Int, val pages: Int, val total: Int)
