package me.anno.fonts.signeddistfields

import me.anno.cache.ICacheData
import me.anno.gpu.texture.ITexture2D
import org.joml.Vector2f

class TextSDF private constructor(val texture: ITexture2D?, val isNull: Boolean, val offset: Vector2f) :
    ICacheData {

    constructor(texture: ITexture2D, offset: Vector2f) : this(texture, false, offset)
    private constructor() : this(null, true, Vector2f())

    val isValid = isNull || texture?.wasCreated == true

    override fun destroy() {
        texture?.destroy()
    }

    companion object {
        val empty = TextSDF()
    }
}