package me.anno.fonts.signeddistfields

import me.anno.gpu.texture.Texture2D
import me.anno.cache.ICacheData
import org.joml.Vector2f

class TextSDF(val texture: Texture2D?, val offset: Vector2f): ICacheData {
    override fun destroy() {
        texture?.destroy()
    }
}