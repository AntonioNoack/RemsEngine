package me.anno.objects.cache

import me.anno.gpu.texture.Texture2D

class TextureCache(val texture: Texture2D?): CacheData {
    override fun destroy() {
        texture?.destroy()
    }
}