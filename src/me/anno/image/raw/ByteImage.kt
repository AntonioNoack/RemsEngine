package me.anno.image.raw

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.maths.Maths.clamp
import me.anno.utils.Logging.hash32
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools.byteBufferPool
import java.nio.ByteBuffer

open class ByteImage(
    width: Int, height: Int,
    val format: ByteImageFormat, val data: ByteArray,
    offset: Int, stride: Int
) : Image(width, height, format.numChannels, format.numChannels > 3, offset, stride) {

    constructor(width: Int, height: Int, format: ByteImageFormat) :
            this(width, height, format, ByteArray(width * height * format.numChannels))

    constructor(width: Int, height: Int, format: ByteImageFormat, data: ByteArray) :
            this(width, height, format, data, 0, format.numChannels * width)

    override fun toString(): String {
        return "${this.javaClass.simpleName}@${hash32(this)}[$width x $height x $format]"
    }

    override fun getIndex(x: Int, y: Int): Int {
        val xi = clamp(x, 0, width - 1)
        val yi = clamp(y, 0, height - 1)
        return offset + xi * numChannels + yi * stride
    }

    override fun getRGB(index: Int): Int {
        return format.fromBytes(data, index, hasAlphaChannel)
    }

    override fun setRGB(index: Int, value: Int) {
        format.toBytes(value, data, index)
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        val buffer = byteBufferPool[data.size, false, false]
        putInto(buffer, true)
        buffer.flip()
        when (format) {
            ByteImageFormat.R -> texture.createMonochrome(buffer, checkRedundancy)
            ByteImageFormat.RG -> texture.createRG(buffer, checkRedundancy)
            ByteImageFormat.RGB -> texture.createRGB(buffer, checkRedundancy)
            ByteImageFormat.BGR -> texture.createBGR(buffer, checkRedundancy)
            ByteImageFormat.ARGB -> texture.createARGB(buffer, checkRedundancy)
            ByteImageFormat.RGBA -> texture.createRGBA(buffer, checkRedundancy)
            ByteImageFormat.BGRA -> texture.createBGRA(buffer, checkRedundancy)
        }
        callback.ok(texture)
    }

    fun putInto(dst: ByteBuffer, flipped: Boolean) {
        val data = data
        val width = width
        val height = height
        val lineLength = width * numChannels
        for (y in 0 until height) {
            val yi = if (flipped) height - 1 - y else y
            dst.put(data, getIndex(0, yi), lineLength)
        }
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return ByteImage(w0, h0, format, data, getIndex(x0, y0), stride)
    }
}