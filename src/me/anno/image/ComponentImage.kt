package me.anno.image

import me.anno.config.DefaultStyle.black
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.image.raw.BIImage
import java.awt.image.BufferedImage

class ComponentImage(val src: Image, val inverse: Boolean, val channel: Char) :
    Image(src.width, src.height, 1, false) {

    val shift = when (channel) {
        'r' -> 16
        'g' -> 8
        'b' -> 0
        else -> 24
    }

    override fun createBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, 1)
        val black = 0xff shl 24
        var i = 0
        when (src) {
            is BIImage -> {
                val data = src.data
                if (inverse) {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            image.setRGB(x, y, (255 - data[i++].shr(shift).and(255)) * 0x10101 + black)
                        }
                    }
                } else {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            image.setRGB(x, y, data[i++].shr(shift).and(255) * 0x10101 + black)
                        }
                    }
                }
            }
            else -> {
                if (inverse) {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            image.setRGB(x, y, (255 - src.getRGB(i++).shr(shift).and(255)) * 0x10101 + black)
                        }
                    }
                } else {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            image.setRGB(x, y, src.getRGB(i++).shr(shift).and(255) * 0x10101 + black)
                        }
                    }
                }
            }
        }
        return image
    }

    override fun createTexture(texture: Texture2D, checkRedundancy: Boolean) {
        val size = width * height
        val bytes = bufferPool[size, false]
        when (src) {
            is BIImage -> {
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
        texture.createMonochrome(bytes, checkRedundancy)
    }

    fun getValue(index: Int): Int {
        val base = src.getRGB(index).ushr(shift).and(255)
        return if (inverse) 255 - base else base
    }

    override fun getRGB(index: Int): Int {
        return (getValue(index) * 0x10101) or black
    }

}