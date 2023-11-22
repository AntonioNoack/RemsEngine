/**
 * Copyright (c) 2009-2021 jMonkeyEngine
 * blablabla,
 *
 * I am trying to support everything, so I'll be extending it
 */
package me.anno.image.tar

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.image.tar.TGAReader.bgra

class TGAImage(// bgra, even if the implementation calls it rgba
    val data: ByteArray, width: Int, height: Int, channels: Int
) : Image(width, height, channels, channels > 3) {

    var originalImageType = 0
    var originalPixelDepth = 0

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        if (sync && GFX.isGFXThread()) {
            when (numChannels) {
                1 -> texture.createMonochrome(data, checkRedundancy)
                2 -> texture.createRG(data, checkRedundancy)
                3 -> texture.createBGR(data, checkRedundancy)
                4 -> texture.createBGRA(data, checkRedundancy)
                else -> throw RuntimeException("$numChannels channels?")
            }
        } else {
            val data = data
            when (numChannels) {
                1 -> {
                    val data2 = if (checkRedundancy) texture.checkRedundancyMonochrome(data) else data
                    val buffer = Texture2D.bufferPool[data2.size, false, false]
                    buffer.put(data2).flip()
                    GFX.addGPUTask("TGAImage", width, height) {
                        if (!texture.isDestroyed)
                            texture.createMonochrome(buffer, false)
                    }
                }
                2 -> {
                    val data2 = if (checkRedundancy) texture.checkRedundancyRG(data) else data
                    val buffer = Texture2D.bufferPool[data.size, false, false]
                    buffer.put(data2).flip()
                    GFX.addGPUTask("TGAImage", width, height) {
                        if (!texture.isDestroyed)
                            texture.createRG(buffer, false)
                    }
                }
                3 -> {
                    val data2 = if (checkRedundancy) texture.checkRedundancyRGB(data) else data
                    val buffer = Texture2D.bufferPool[data2.size, false, false]
                    buffer.put(data2).flip()
                    GFX.addGPUTask("TGAImage", width, height) {
                        if (!texture.isDestroyed)
                            texture.createBGR(buffer, false)
                    }
                }
                4 -> {
                    val data2 = if (checkRedundancy) texture.checkRedundancy(data) else data
                    val buffer = Texture2D.bufferPool[data2.size, false, false]
                    buffer.put(data2).flip()
                    GFX.addGPUTask("TGAImage", width, height) {
                        if (!texture.isDestroyed)
                            texture.createBGRA(buffer, false)
                    }
                }
                else -> throw RuntimeException("$numChannels channels?")
            }
        }
    }

    override fun getRGB(index: Int): Int {
        return when (numChannels) {
            1 -> 0x10101 * (data[index].toInt() and 255)
            2 -> {
                val j = index * 2
                argb(255, data[j + 1].toInt(), data[j].toInt(), 0)
            }
            3 -> {
                val j = index * 3
                argb(255, data[j + 2].toInt(), data[j + 1].toInt(), data[j].toInt())
            }
            4 -> {
                val j = index * 4
                bgra(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), data[j + 3].toInt())
            }
            else -> throw RuntimeException("$numChannels is not supported for TGA images")
        }
    }

    override fun createIntImage(): IntImage {
        val width = width
        val height = height
        val channels = numChannels
        if (channels != 1 && channels != 3 && channels != 4)
            return super.createIntImage()
        val size = width * height
        val data = data
        val dst = IntArray(size)
        when (channels) {
            1 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    dst[i++] = 0x10101 * (data[j++].toInt() and 255)
                }
            }
            3 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    dst[i++] = bgra(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), 255)
                    j += 3
                }
            }
            4 -> {
                var i = 0
                var j = 0
                while (i < size) {
                    dst[i++] = bgra(data[j].toInt(), data[j + 1].toInt(), data[j + 2].toInt(), data[j + 3].toInt())
                    j += 4
                }
            }
        }
        return IntImage(width, height, dst, hasAlphaChannel)
    }
}