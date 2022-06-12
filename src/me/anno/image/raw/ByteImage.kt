package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.rgb
import me.anno.utils.Color.rgba

/**
 * image class with ByteArray data;
 * @param rgba set this to true, if your image is 4 channel RGBA, and to false if it is 4 channel ARGB; ignored for < 4 channels
 * */
open class ByteImage(
    width: Int, height: Int,
    channelsInData: Int,
    val data: ByteArray = ByteArray(width * height * channelsInData),
    val rgba: Boolean = true,
    hasAlphaChannel: Boolean = channelsInData > 3
) : Image(width, height, channelsInData, hasAlphaChannel) {

    constructor(width: Int, height: Int, channelsInData: Int, rgba: Boolean, hasAlphaChannel: Boolean) :
            this(width, height, channelsInData, ByteArray(width * height * channelsInData), rgba, hasAlphaChannel)

    override fun getRGB(index: Int): Int {
        return when (numChannels) {
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
                if (rgba) {
                    val i = index * 4
                    rgba(data[i], data[i + 1], data[i + 2], data[i + 3])
                } else { // ARGB
                    val i = index * 4
                    argb(data[i], data[i + 1], data[i + 2], data[i + 3])
                }
            }
            else -> throw RuntimeException("Only 1..4 channels are supported")
        }
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        // todo optimize for async scenario
        if (!GFX.isGFXThread()) {
            GFX.addGPUTask("ByteImage", width, height) {
                createTexture(texture, true, checkRedundancy)
            }
            return
        }
        when (numChannels) {
            1 -> texture.createMonochrome(data, checkRedundancy)
            2 -> texture.createRG(data, checkRedundancy)
            3 -> createRGBFrom3StridedData(texture, width, height, checkRedundancy, data)
            4 -> {
                if (hasAlphaChannel && hasAlpha(data)) {
                    if (rgba) texture.createRGBA(data, checkRedundancy)
                    else texture.createARGB(data, checkRedundancy)
                } else {
                    if (rgba) texture.create(TargetType.UByteTarget3, TargetType.UByteTarget4, data)
                    else {
                        // todo we don't need alpha here
                        texture.createARGB(data, checkRedundancy)
                    }
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