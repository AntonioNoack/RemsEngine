package me.anno.cache.data

import me.anno.gpu.texture.ITexture2D

class LateinitTexture : ICacheData {
    var texture: ITexture2D? = null
        set(value) {
            if(isDestroyed) value?.destroy()
            field = value
        }
    var isDestroyed = false
    override fun destroy() {
        isDestroyed = true
        texture?.destroy()
    }
}