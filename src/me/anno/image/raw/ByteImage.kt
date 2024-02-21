package me.anno.image.raw

import me.anno.utils.structures.Callback
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.rgb
import me.anno.utils.Color.rgba

open class ByteImage(
    width: Int, height: Int,
    val format: Format,
    val data: ByteArray = ByteArray(width * height * format.numChannels),
) : Image(width, height, format.numChannels, format.numChannels > 3) {

    enum class Format(val numChannels: Int) {
        R(1),
        RG(2),
        RGB(3), BGR(3),
        RGBA(4), ARGB(4), BGRA(4),
    }

    constructor(width: Int, height: Int, format: Format) :
            this(width, height, format, ByteArray(width * height * format.numChannels))

    override fun getRGB(index: Int): Int {
        return when (format) {
            Format.R -> data[index].toInt().and(255) * 0x10101
            Format.RG -> {
                val i = index * 2
                rgb(data[i], data[i + 1], 0)
            }
            Format.RGB -> {
                val i = index * 3
                rgb(data[i], data[i + 1], data[i + 2])
            }
            Format.BGR -> {
                val i = index * 3
                rgb(data[i + 2], data[i + 1], data[i])
            }
            Format.RGBA -> {
                val i = index * 4
                val a = if (hasAlphaChannel) data[i + 3] else -1
                rgba(data[i], data[i + 1], data[i + 2], a)
            }
            Format.ARGB -> {
                val i = index * 4
                val a = if (hasAlphaChannel) data[i + 3] else -1
                argb(data[i], data[i + 1], data[i + 2], a)
            }
            Format.BGRA -> {
                val i = index * 4
                val b = data[i]
                val g = data[i + 1]
                val r = data[i + 2]
                val a = if (hasAlphaChannel) data[i + 3] else -1
                argb(a, r, g, b)
            }
        }
    }

    override fun createTexture(
        texture: Texture2D,
        sync: Boolean,
        checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        if (!GFX.isGFXThread()) {
            GFX.addGPUTask("ByteImage", width, height) {
                createTexture(texture, true, checkRedundancy, callback)
            }
        } else {
            when (format) {
                Format.R -> texture.createMonochrome(data, checkRedundancy)
                Format.RG -> texture.createRG(data, checkRedundancy)
                Format.RGB -> texture.createRGB(data, checkRedundancy)
                Format.BGR -> texture.createBGR(data, checkRedundancy)
                Format.ARGB -> texture.createARGB(data, checkRedundancy)
                Format.RGBA -> {
                    if (hasAlphaChannel && hasAlpha(data)) texture.createRGBA(data, checkRedundancy)
                    else texture.create(TargetType.UInt8x3, TargetType.UInt8x4, data)
                }
                Format.BGRA -> {
                    texture.createBGRA(data, checkRedundancy)
                }
            }
            callback.ok(texture)
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