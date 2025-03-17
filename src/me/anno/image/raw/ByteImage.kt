package me.anno.image.raw

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
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
            this(width, height, format, data, 0, width)

    override fun getRGB(index: Int): Int {
        return format.fromBytes(data, index * format.numChannels, hasAlphaChannel)
    }

    fun setRGB(x: Int, y: Int, rgb: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        format.toBytes(rgb, data, getIndex(x, y) * format.numChannels)
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
        val numChannels = numChannels
        for (y in 0 until height) {
            val yi = if (flipped) height - 1 - y else y
            dst.put(data, getIndex(0, yi) * numChannels, width * numChannels)
        }
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return ByteImage(w0, h0, format, data, getIndex(x0, y0), stride)
    }
}