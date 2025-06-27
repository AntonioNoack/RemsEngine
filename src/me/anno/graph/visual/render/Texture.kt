package me.anno.graph.visual.render

import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.MultiFramebuffer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.IndestructibleTexture2D
import me.anno.gpu.texture.LazyTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.assertions.assertFail
import org.joml.Vector4f
import kotlin.math.max

class Texture(
    val tex: ITexture2D,
    texMS: ITexture2D?,
    val mapping: String,
    val encoding: DeferredLayerType?
) {

    constructor(v: ITexture2D) : this(v, v, "", null)
    constructor(v: Vector4f) : this(IndestructibleTexture2D.getColorTexture(v))

    val texMS = texMS ?: tex
    val isDestroyed get() = tex.isDestroyed

    override fun toString(): String {
        return when (tex) {
            whiteTexture -> "white"
            blackTexture -> "black"
            else -> {
                val base = if (tex is Texture2D) "'${tex.name}', ${tex.width}x${tex.height}@${tex.pointer}"
                else "'${tex.name}', ${tex.width}x${tex.height}"
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

        private val mask1s = listOf(
            Vector4f(1f, 0f, 0f, 0f),
            Vector4f(0f, 1f, 0f, 0f),
            Vector4f(0f, 0f, 1f, 0f),
            Vector4f(0f, 0f, 0f, 1f),
        )

        fun mask1(str: String?): Vector4f {
            return mask1s[mask1Index(str)]
        }

        fun mask1Index(str: String?): Int {
            if (str == null) return 0
            val index = max("xyzw".indexOf(str), "rgba".indexOf(str))
            return max(index, 0)
        }

        /**
         * Whether the value (must have two components!) is mapped onto zw instead of xy.
         * */
        val Texture?.isZWMapping get() = this?.mapping == "zw"

        val Texture?.texOrNull get() = if (this != null && tex.isCreated()) tex else null
        val Texture?.texMSOrNull get() = if (this != null && texMS.isCreated()) texMS else texOrNull

        val Texture?.mask: Vector4f
            get() = mask1(this?.mapping)

        val Texture?.mask1Index: Int
            get() = mask1Index(this?.mapping)

        fun texture(f: IFramebuffer, i: Int): Texture {
            return texture(f, i, "", null)
        }

        fun texture(f: IFramebuffer, i: Int, mapping: String, type: DeferredLayerType?): Texture {
            return if (f.samples <= 1) Texture(f.getTextureI(i), null, mapping, type)
            else Texture(f.getTextureILazy(i), f.getTextureIMS(i), mapping, type)
        }

        fun texture(f: IFramebuffer, settings: DeferredSettings, type: DeferredLayerType): Texture {
            val layer = settings.findLayer(type)
            return if (layer != null) {
                val i = layer.texIndex
                val mapping = layer.mapping
                texture(f, i, mapping, type)
            } else if (type == DeferredLayerType.DEPTH && GFX.supportsDepthTextures) {
                depth(f)
            } else assertFail("Missing $type in $settings")
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

            val texLazy = LazyTexture(tex, texMS, lazy {
                val f1st = (f as? Framebuffer) ?: (f as? MultiFramebuffer)?.targetsI?.first()!!
                f1st.copyIfNeeded(f1st.ssBuffer!!, 1 shl f1st.targets.size)
            })
            return Texture(texLazy, texMS, mapping, type)
        }
    }
}