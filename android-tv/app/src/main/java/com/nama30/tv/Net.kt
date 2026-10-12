package com.nama30.tv

import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** One shared HTTP client for the API bridge and the video player. */
object Net {
    /** 30nama sits behind Cloudflare; present as the Tizen TV client the web app was built for. */
    const val UA = "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) 85.0.4183.93/6.5 TV Safari/537.36"

    @Volatile private var client: OkHttpClient? = null

    fun client(cacheDir: File): OkHttpClient = client ?: synchronized(this) {
        client ?: OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { ch ->
                val r = ch.request()
                ch.proceed(if (r.header("User-Agent") == null) r.newBuilder().header("User-Agent", UA).build() else r)
            }
            .cache(Cache(File(cacheDir, "http"), 32L * 1024 * 1024))
            .build().also { client = it }
    }
}
