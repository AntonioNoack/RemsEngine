package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.rgb
import me.anno.utils.Color.rgba
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

open class ByteImage(
    width: Int, height: Int,
    val channelsInData: Int,
    val data: ByteArray = ByteArray(width * height * channelsInData),
    hasAlphaChannel: Boolean = channelsInData > 3
) : Image(width, height, channelsInData, hasAlphaChannel) {

    constructor(width: Int, height: Int, channelsInData: Int, hasAlphaChannel: Boolean) :
            this(width, height, channelsInData, ByteArray(width * height * channelsInData), hasAlphaChannel)

    override fun getRGB(index: Int): Int {
        return when (channelsInData) {
            1 -> data[index].toInt().and(255) * 0x10101
            2 -> {
                val i = index * 2
                rgb(data[i], data[i + 1], 0)
            }
            3 -> {
                val i = index * 3
                rgb(data[i], data[i + 1], data[i + 2])
            }
            4 -> {
                val i = index * 4
                rgba(data[i + 1], data[i + 2], data[i + 3], data[i])
            }
            else -> throw RuntimeException("Only 1..4 channels are supported")
        }
    }

    override fun createBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
        val src = data
        val raster = image.raster
        val dst = (raster.dataBuffer as DataBufferInt).data
        when (channelsInData) {
            1 -> {
                for (i in 0 until width * height) {
                    dst[i] = src[i].toInt().and(255) * 0x10101
                }
            }
            else -> {
                for (i in 0 until width * height) {
                    dst[i] = getRGB(i)
                }
            }
        }
        return image
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        // todo optimize for async scenario
        if (!GFX.isGFXThread()) {
            GFX.addGPUTask("ByteImage", width, height) {
                createTexture(texture, true, checkRedundancy)
            }
            return
        }
        when (channelsInData) {
            1 -> texture.createMonochrome(data, checkRedundancy)
            2 -> texture.createRG(data, checkRedundancy)
            3 -> createRGBFrom3StridedData(texture, width, height, checkRedundancy, data)
            4 -> {
                if (hasAlphaChannel && hasAlpha(data)) {
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
            val v255 = (-1).toByte()
            for (i in 0 until (data.size shr 2)) {
                if (data[i shl 2] != v255) {
                    return true
                }
            }
            return false
        }
    }

}