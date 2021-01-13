package me.anno.fonts.signeddistfields

import me.anno.cache.ICacheData
import me.anno.gpu.texture.Texture2D
import org.joml.Vector2f

class TextSDF private constructor(val texture: Texture2D?, val isNull: Boolean, val offset: Vector2f) : ICacheData {

    constructor(texture: Texture2D, offset: Vector2f) : this(texture, false, offset)
    private constructor() : this(null, true, Vector2f())

    val isValid = isNull || texture?.isCreated == true

    override fun destroy() {
        texture?.destroy()
    }

    companion object {
        val empty = TextSDF()
    }

}