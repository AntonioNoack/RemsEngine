package me.anno.graph.render

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.white4
import org.joml.Vector4f

class Texture private constructor(
    val tex: ITexture2D,
    val texMS: ITexture2D?,
    val mapping: String,
    val encoding: DeferredLayerType?,
    val color: Vector4f
) {

    constructor(v: ITexture2D) : this(v, null, "", null, white4)
    constructor(v: Vector4f) : this(whiteTexture, null, "", null, v)
    constructor(tex: ITexture2D, texMS: ITexture2D?, mapping: String, encoding: DeferredLayerType?) :
            this(tex, texMS, mapping, encoding, white4)

    val isDestroyed get() = tex is Texture2D && tex.isDestroyed

    override fun toString(): String {
        return if (tex == whiteTexture) if (color == white4) "white" else color.toHexColor()
        else {
            val base = if (tex is Texture2D) "${tex.width}x${tex.height}@${tex.pointer}"
            else "${tex.width}x${tex.height}}"
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

    companion object {

        fun texture(f: IFramebuffer, i: Int): Texture {
            return texture(f, i, "", null)
        }

        fun texture(f: IFramebuffer, i: Int, mapping: String, type: DeferredLayerType?): Texture {
            return if (f.samples <= 1) Texture(f.getTextureI(i), null, mapping, type)
            else Texture(f.getTextureI(i), f.getTextureIMS(i), mapping, type)
        }

        fun depth(f: IFramebuffer): Texture {
            return depth(f, "x", DeferredLayerType.DEPTH)
        }

        fun depth(f: IFramebuffer, mapping: String, type: DeferredLayerType?): Texture {
            if (f.samples <= 1) return Texture(f.depthTexture!!, null, mapping, type)

            val buf0 = (f as? Framebuffer)?.ssBuffer
            val buf1 = (f as? MultiFramebuffer)?.targetsI?.first()?.ssBuffer
            val tex = (buf0 ?: buf1 ?: f).depthTexture!!
            val texMS = f.depthTexture!!

            val f1st = (f as? Framebuffer) ?: (f as? MultiFramebuffer)?.targetsI?.first()!!
            f1st.copyIfNeeded(f1st.ssBuffer!!)

            return Texture(tex, texMS, mapping, type)
        }
    }
}