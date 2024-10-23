package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Redundancy.checkRedundancyX1
import me.anno.gpu.texture.Redundancy.checkRedundancyX2
import me.anno.gpu.texture.Redundancy.checkRedundancyX3
import me.anno.gpu.texture.Redundancy.checkRedundancyX4
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.argb
import me.anno.utils.Color.rgb
import me.anno.utils.Color.rgba
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools.byteBufferPool
import java.nio.ByteBuffer

open class ByteImage(
    width: Int, height: Int,
    val format: Format, val data: ByteArray,
    offset: Int, stride: Int
) : Image(width, height, format.numChannels, format.numChannels > 3, offset, stride) {

    enum class Format(val numChannels: Int) {
        R(1),
        RG(2),
        RGB(3), BGR(3),
        RGBA(4), ARGB(4), BGRA(4),
    }

    constructor(width: Int, height: Int, format: Format) :
            this(width, height, format, ByteArray(width * height * format.numChannels))

    constructor(width: Int, height: Int, format: Format, data: ByteArray) :
            this(width, height, format, data, 0, width)

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
        texture: Texture2D, sync: Boolean,
        checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) = createTexture(texture, checkRedundancy, data, callback)

    private fun createTexture(
        texture: Texture2D, checkRedundancy: Boolean,
        data: ByteArray, callback: Callback<ITexture2D>
    ) {
        if (!GFX.isGFXThread()) {
            val data2 = if (checkRedundancy) {
                when (format.numChannels) {
                    1 -> texture.checkRedundancyX1(data)
                    2 -> texture.checkRedundancyX2(data)
                    3 -> texture.checkRedundancyX3(data)
                    else -> texture.checkRedundancyX4(data)
                }
            } else data
            val buffer = byteBufferPool[data2.size, false, false]
            buffer.put(data2)
            buffer.flip()
            addGPUTask("ByteImage $width x $height", width, height) {
                createTexture(texture, false, buffer, callback)
                byteBufferPool.returnBuffer(buffer)
            }
        } else {
            val buffer = byteBufferPool[data.size, false, false]
            buffer.put(data)
            buffer.flip()
            createTexture(texture, checkRedundancy, buffer, callback)
        }
    }

    private fun createTexture(
        texture: Texture2D, checkRedundancy: Boolean,
        data: ByteBuffer, callback: Callback<ITexture2D>
    ) {
        when (format) {
            Format.R -> texture.createMonochrome(data, checkRedundancy)
            Format.RG -> texture.createRG(data, checkRedundancy)
            Format.RGB -> texture.createRGB(data, checkRedundancy)
            Format.BGR -> texture.createBGR(data, checkRedundancy)
            Format.ARGB -> texture.createARGB(data, checkRedundancy)
            Format.RGBA -> texture.createRGBA(data, checkRedundancy)
            Format.BGRA -> texture.createBGRA(data, checkRedundancy)
        }
        callback.ok(texture)
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return ByteImage(w0, h0, format, data, getIndex(x0, y0), stride)
    }
}