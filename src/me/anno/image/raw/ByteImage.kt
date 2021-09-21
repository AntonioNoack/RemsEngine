package me.anno.image.raw

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.rgba
import java.awt.image.BufferedImage

open class ByteImage(
    width: Int, height: Int,
    val channelsInData: Int,
    val data: ByteArray = ByteArray(width * height * 4),
    hasAlphaChannel: Boolean = false
) : Image(channelsInData, hasAlphaChannel) {

    init {
        this.width = width
        this.height = height
    }

    override fun getRGB(index: Int): Int {
        return when (channelsInData) {
            1 -> data[index].toInt().and(255) * 0x10101
            2 -> data[index * 2].toInt().and(255).shl(8) +
                    data[index * 2 + 1].toInt().and(255)
            3 -> data[index * 3].toInt().and(255).shl(16) +
                    data[index * 3 + 1].toInt().and(255).shl(8) +
                    data[index * 3 + 2].toInt().and(255)
            4 -> {
                var i = index * 4
                val a = data[i++]
                val r = data[i++]
                val g = data[i++]
                val b = data[i]
                rgba(r, g, b, a)
            }
            else -> throw RuntimeException("Only 1..4 channels are supported")
        }
    }

    override fun createBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
        var i = 0
        when (channelsInData) {
            1 -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        image.setRGB(x, y, data[i].toInt().and(255) * 0x10101)
                    }
                }
            }
            else -> {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        image.setRGB(x, y, getRGB(i++))
                    }
                }
            }
        }
        return image
    }

    override fun createTexture(texture: Texture2D, checkRedundancy: Boolean) {
        when (channelsInData) {
            1 -> texture.createMonochrome(data, checkRedundancy)
            2 -> texture.createRG(data, checkRedundancy)
            3 -> createRGBFrom3StridedData(texture, checkRedundancy, data)
            4 -> {
                if (hasAlphaChannel) {
                    texture.createRGBA(data, checkRedundancy)
                } else {
                    texture.createRGB(data, checkRedundancy)
                }
            }
            else -> throw RuntimeException()
        }
    }

    companion object {
        fun hasAlpha(data: ByteArray): Boolean {
            for (i in 0 until data.size / 4) {
                if (data[i * 4] != 0.toByte()) {
                    return true
                }
            }
            return false
        }
    }

}