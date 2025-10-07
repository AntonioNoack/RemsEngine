package me.anno.image.raw

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.maths.MinMax.min
import me.anno.utils.Color.black
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools

open class IntImage(
    width: Int, height: Int,
    val data: IntArray = IntArray(width * height),
    hasAlphaChannel: Boolean, offset: Int, stride: Int
) : Image(width, height, if (hasAlphaChannel) 4 else 3, hasAlphaChannel, offset, stride) {

    constructor(width: Int, height: Int, data: IntArray, hasAlphaChannel: Boolean) :
            this(width, height, data, hasAlphaChannel, 0, width)

    constructor(width: Int, height: Int, hasAlphaChannel: Boolean) :
            this(width, height, IntArray(width * height), hasAlphaChannel)

    override fun setRGB(index: Int, value: Int) {
        data[index] =
            if (hasAlphaChannel) value
            else (value or black)
    }

    override fun getRGB(index: Int): Int {
        return if (hasAlphaChannel) data[index]
        else (data[index] or black)
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        // data cloning is required, because the function in Texture2D switches the red and blue channels
        val flipped = true
        if (hasAlphaChannel) texture.createBGRA(cloneData(flipped), checkRedundancy)
        else texture.createBGR(cloneData(flipped), checkRedundancy)
        callback.ok(texture)
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return IntImage(w0, h0, data, hasAlphaChannel, getIndex(x0, y0), stride)
    }

    fun fillAlpha(alpha: Int) {
        val data = data
        val alphaI = alpha shl 24
        for (i in data.indices) {
            data[i] = data[i].and(0xffffff) or alphaI
        }
    }

    fun copyInto(dst: IntImage, x0: Int, y0: Int) {
        val srcData = data
        val dstData = dst.data
        val width = min(width, dst.width - x0)
        val height = min(height, dst.height - y0)
        if (width <= 0) return
        for (y in 0 until height) {
            val srcI0 = getIndex(0, y)
            val dstI0 = dst.getIndex(x0, y0 + y)
            srcData.copyInto(dstData, dstI0, srcI0, srcI0 + width)
        }
    }

    override fun asIntImage(): IntImage = this

    override fun cloneToIntImage(): IntImage {
        val clone = IntImage(width, height, hasAlphaChannel)
        copyInto(clone, 0, 0)
        return clone
    }

    fun cloneData(flipped: Boolean = false): IntArray {
        val clone = Pools.intArrayPool[width * height, false, true]
        for (y in 0 until height) {
            val i0 = getIndex(0, if (flipped) height - 1 - y else y)
            data.copyInto(clone, y * width, i0, i0 + width)
        }
        return clone
    }
}