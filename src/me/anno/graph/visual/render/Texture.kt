package me.anno.graph.visual.render

import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.deferred.DeferredSettings.Companion.singleToVector
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.IndestructibleTexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import org.joml.Vector4f

class Texture private constructor(
    val tex: ITexture2D,
    texMS: ITexture2D?,
    val mapping: String,
    val encoding: DeferredLayerType?
) {

    constructor(v: ITexture2D) : this(v, v, "", null)
    constructor(v: Vector4f) : this(IndestructibleTexture2D.getColorTexture(v))

    val texMS = texMS ?: tex
    val isDestroyed get() = tex.isDestroyed
    val mask get() = singleToVector[mapping]
    val texOrNull get() = if (tex.isCreated()) tex else null
    val texMSOrNull get() = if (texMS.isCreated()) texMS else null

    override fun toString(): String {
        return when (tex) {
            whiteTexture -> "white"
            blackTexture -> "black"
            else -> {
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
    }

    companion object {

        fun texture(f: IFramebuffer, i: Int): Texture {
            return texture(f, i, "", null)
        }

        fun texture(f: IFramebuffer, i: Int, mapping: String, type: DeferredLayerType?): Texture {
            return if (f.samples <= 1) Texture(f.getTextureI(i), null, mapping, type)
            else Texture(f.getTextureI(i), f.getTextureIMS(i), mapping, type)
        }

        fun texture(f: IFramebuffer, settings: DeferredSettings, type: DeferredLayerType): Texture {
            val layer = settings.findLayer(type)
            return if (layer != null) {
                val i = layer.texIndex
                val mapping = layer.mapping
                texture(f, i, mapping, type)
            } else if (type == DeferredLayerType.DEPTH && GFX.supportsDepthTextures) {
                depth(f)
            } else throw IndexOutOfBoundsException("Missing $type in $settings")
        }

        fun depth(f: IFramebuffer): Texture {
            return depth(f, "x", DeferredLayerType.DEPTH)
        }

        fun depth(f: IFramebuffer, mapping: String, type: DeferredLayerType?): Texture {
            if (!GFX.supportsDepthTextures) return texture(f, f.numTextures - 1, mapping, type) // via FBStack
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