package me.anno.fonts.signeddistfields

import me.anno.cache.ICacheData
import me.anno.gpu.texture.ITexture2D
import org.joml.Vector2f

class TextSDF(val texture: ITexture2D?, val offset: Vector2f) : ICacheData {
    override fun destroy() {
        texture?.destroy()
    }

    companion object {
        val empty = TextSDF(null, Vector2f())
    }
}