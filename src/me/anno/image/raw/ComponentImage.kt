package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType.Companion.Float16x4
import me.anno.gpu.framebuffer.TargetType.Companion.Float32x4
import me.anno.gpu.framebuffer.TargetType.Companion.UInt8x4
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.gpu.texture.Texture2D.Companion.getNumberType
import me.anno.image.Image
import me.anno.utils.Color.black
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C.GL_FLOAT
import org.lwjgl.opengl.GL46C.GL_HALF_FLOAT

/**
 * maps a component like R/G/B/A onto RGB1 (opaque, grayscale)
 * */
class ComponentImage(val src: Image, val inverse: Boolean, val channel: Char) :
    Image(src.width, src.height, 1, false) {

    companion object {
        private val LOGGER = LogManager.getLogger(ComponentImage::class)
    }

    private val shift = "bgra".indexOf(channel) * 8

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        if (src is GPUImage) {
            val m = if (inverse) channel.uppercaseChar() else channel
            val tex = src.texture
            val type = if (tex is Texture2D) {
                when (getNumberType(tex.internalFormat)) {
                    GL_FLOAT -> Float32x4
                    GL_HALF_FLOAT -> Float16x4
                    else -> UInt8x4
                }
            } else UInt8x4
            TextureMapper.mapTexture(
                src.texture, texture, "$m$m${m}1",
                type, callback
            )
        } else {
            val size = width * height
            val bytes = bufferPool[size, false, false]
            when (src) {
                is IntImage -> {
                    // use direct data access
                    val data = src.data
                    if (inverse) {
                        for (i in 0 until size) {
                            bytes.put(i, (255 - data[i].shr(shift)).toByte())
                        }
                    } else {
                        for (i in 0 until size) {
                            bytes.put(i, data[i].shr(shift).toByte())
                        }
                    }
                }
                else -> {
                    if (inverse) {
                        for (i in 0 until size) {
                            bytes.put(i, (255 - src.getRGB(i).shr(shift)).toByte())
                        }
                    } else {
                        for (i in 0 until size) {
                            bytes.put(i, src.getRGB(i).shr(shift).toByte())
                        }
                    }
                }
            }
            if (sync && GFX.isGFXThread()) {
                texture.createMonochrome(bytes, checkRedundancy)
                callback(texture, null)
            } else {
                if (checkRedundancy) texture.checkRedundancyMonochrome(bytes)
                GFX.addGPUTask("ComponentImage", width, height) {
                    if (!texture.isDestroyed) {
                        texture.createMonochrome(bytes, checkRedundancy = false)
                        callback(texture, null) // callback in both cases?...
                    } else LOGGER.warn("Image was already destroyed")
                }
            }
        }
    }

    private fun getValue(index: Int): Int {
        val base = src.getRGB(index).ushr(shift).and(255)
        return if (inverse) 255 - base else base
    }

    override fun getRGB(index: Int): Int {
        return (getValue(index) * 0x10101) or black
    }

    override fun toString(): String {
        return "ComponentImage { $src, ${if (inverse) "1-" else ""}$channel }"
    }
}