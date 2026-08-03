package com.example.barracloud

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.barracloud.data.repository.MediaRepository

class BarraCloudApplication : Application(), ImageLoaderFactory {

    lateinit var repository: MediaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = MediaRepository(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("barra_image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB disk cache
                    .build()
            }
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: BarraCloudApplication
            private set
    }
}
