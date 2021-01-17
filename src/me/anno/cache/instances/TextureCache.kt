package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.LateinitTexture
import me.anno.gpu.texture.ITexture2D

object TextureCache : CacheSection("Textures") {
    fun getLateinitTexture(
        key: Any,
        timeout: Long,
        generator: (callback: (ITexture2D?) -> Unit) -> Unit
    ): LateinitTexture {
        return getEntry(key, timeout, false) {
            LateinitTexture().apply {
                generator {
                    texture = it
                }
            }
        } as LateinitTexture
    }
}