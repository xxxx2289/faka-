package cn.xiaobai.acgfaka.user

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * 全局 Application。配置 Coil 图片加载器的内存 + 磁盘缓存，
 * 商品图首次加载后落本地磁盘，二次进入不再回服务器，减轻服务器负担。
 */
class App : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024) // 256MB 磁盘缓存
                    .build()
            }
            .crossfade(true)
            .build()
}
