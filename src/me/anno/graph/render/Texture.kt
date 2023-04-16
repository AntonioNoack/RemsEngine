package me.anno.graph.render

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.white4
import org.joml.Vector4f

class Texture private constructor(
    val tex: ITexture2D,
    val mapping: String,
    val encoding: DeferredLayerType?,
    val color: Vector4f
) {
    constructor(v: ITexture2D) : this(v, "", null, white4)
    constructor(v: Vector4f) : this(whiteTexture, "", null, v)
    constructor(tex: ITexture2D, mapping: String, encoding: DeferredLayerType?) : this(tex, mapping, encoding, white4)

    val isDestroyed get() = tex is Texture2D && tex.isDestroyed

    override fun toString(): String {
        return if (tex == whiteTexture) if (color == white4) "white" else color.toHexColor()
        else {
            val base = if (tex is Texture2D) "${tex.w}x${tex.h}@${tex.pointer}"
            else "${tex.w}x${tex.h}}"
            val hasMap = mapping.isNotEmpty()
            val hasEnc = encoding != null && encoding.dataToWork.isNotEmpty()
            when {
                hasMap && hasEnc -> "$base.$mapping/${encoding!!.name}"
                hasMap -> "$base.$mapping"
                hasEnc -> "$base/${encoding!!.name}"
                else -> base
            }
        }
    }
}