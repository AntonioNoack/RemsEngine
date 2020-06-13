package me.anno.objects.cache

import me.anno.gpu.texture.Texture3D

class Texture3DCache(var texture: Texture3D?): CacheData {
    override fun destroy() {
        texture?.destroy()
    }
}