package me.anno.objects.cache

import me.anno.gpu.texture.ITexture2D

class TextureCache(var texture: ITexture2D?): CacheData {
    override fun destroy() {
        texture?.destroy()
    }
}