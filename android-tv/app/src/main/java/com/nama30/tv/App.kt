package com.nama30.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.nama30.tv.data.Prefs

class App : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }

    /** Posters are small and opaque: RGB_565 halves their bitmap memory; the memory cache is capped at 15% of the heap. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { Net.client(cacheDir) }
        .allowRgb565(true)
        .crossfade(150)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("img")).maxSizeBytes(150L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .apply { if (BuildConfig.DEBUG) logger(coil.util.DebugLogger()) }
        .build()
}
