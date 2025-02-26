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
            ByteImageFormat.R -> texture.createMonochrome(data, checkRedundancy)
            ByteImageFormat.RG -> texture.createRG(data, checkRedundancy)
            ByteImageFormat.RGB -> texture.createRGB(data, checkRedundancy)
            ByteImageFormat.BGR -> texture.createBGR(data, checkRedundancy)
            ByteImageFormat.ARGB -> texture.createARGB(data, checkRedundancy)
            ByteImageFormat.RGBA -> texture.createRGBA(data, checkRedundancy)
            ByteImageFormat.BGRA -> texture.createBGRA(data, checkRedundancy)
        }
        callback.ok(texture)
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return ByteImage(w0, h0, format, data, getIndex(x0, y0), stride)
    }
}