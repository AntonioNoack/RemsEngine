package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.image.Image
import me.anno.utils.Color.black
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

class ComponentImage(val src: Image, val inverse: Boolean, val channel: Char) :
    Image(src.width, src.height, 1, false) {

    companion object {
        private val LOGGER = LogManager.getLogger(ComponentImage::class)
    }

    val shift = when (channel) {
        'r' -> 16
        'g' -> 8
        'b' -> 0
        else -> 24
    }

    override fun createBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        val buffer = image.data.dataBuffer as DataBufferByte
        val data1 = buffer.data
        val shift = shift
        when (src) {
            is IntImage -> {
                val data = src.data
                if (inverse) {
                    for (i in 0 until width * height) {
                        data1[i] = (255 - data[i].shr(shift)).toByte()
                    }
                } else {
                    for (i in 0 until width * height) {
                        data1[i] = data[i].shr(shift).toByte()
                    }
                }
            }
            else -> {
                if (inverse) {
                    for (i in 0 until width * height) {
                        data1[i] = (255 - src.getRGB(i).shr(shift)).toByte()
                    }
                } else {
                    for (i in 0 until width * height) {
                        data1[i] = src.getRGB(i).shr(shift).toByte()
                    }
                }
            }
        }
        return image
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
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
            is GPUImage -> {
                TODO("use shader to create texture")
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
        } else {
            if (checkRedundancy) texture.checkRedundancyMonochrome(bytes)
            GFX.addGPUTask("ComponentImage", width, height) {
                if (!texture.isDestroyed) {
                    texture.createMonochrome(bytes, checkRedundancy = false)
                } else LOGGER.warn("Image was already destroyed")
            }
        }
    }

    fun getValue(index: Int): Int {
        val base = src.getRGB(index).ushr(shift).and(255)
        return if (inverse) 255 - base else base
    }

    override fun getRGB(index: Int): Int {
        return (getValue(index) * 0x10101) or black
    }
}