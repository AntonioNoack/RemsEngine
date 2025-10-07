package me.anno.fonts.signeddistfields

import me.anno.cache.ICacheData
import me.anno.gpu.texture.ITexture2D
import org.joml.AABBf

class TextSDF(
    val texture: ITexture2D?, val bounds: AABBf,
    val codepoint: Int, val z: Float, val color: Int
) : ICacheData {
    override fun destroy() {
        texture?.destroy()
    }

    companion object {
        val empty = TextSDF(null, AABBf(), 0, 0f, 0)
    }
}