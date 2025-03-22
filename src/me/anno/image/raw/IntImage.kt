package me.anno.image.raw

import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.maths.Maths
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.posMod
import me.anno.utils.Color.a01
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools
import kotlin.math.abs
import kotlin.math.floor

open class IntImage(
    width: Int, height: Int,
    val data: IntArray = IntArray(width * height),
    hasAlphaChannel: Boolean, offset: Int, stride: Int
) : Image(width, height, if (hasAlphaChannel) 4 else 3, hasAlphaChannel, offset, stride) {

    constructor(width: Int, height: Int, data: IntArray, hasAlphaChannel: Boolean) :
            this(width, height, data, hasAlphaChannel, 0, width)

    constructor(width: Int, height: Int, hasAlphaChannel: Boolean) :
            this(width, height, IntArray(width * height), hasAlphaChannel)

    fun setRGB(x: Int, y: Int, rgb: Int) {
        setRGB(getIndex(x, y), rgb)
    }

    fun setRGB(index: Int, rgb: Int) {
        data[index] =
            if (hasAlphaChannel) rgb
            else (rgb or black)
    }

    fun mixRGB(x: Float, y: Float, rgb: Int, alpha: Float = rgb.a01(), clamping: Clamping = Clamping.REPEAT) {
        if (x.isNaN() || y.isNaN()) return
        when (clamping) {
            Clamping.REPEAT -> if (x in 0f..width.toFloat() && y in 0f..height.toFloat()) {
                val xf = min(floor(x), width - 1f)
                val yf = min(floor(y), height - 1f)
                val gx = (x - xf) * alpha
                val fx = alpha - gx
                val gy = (y - yf) * alpha
                val fy = alpha - gy
                val x0 = xf.toInt()
                val x1 = if (x0 + 1 >= width) 0 else x0 + 1
                val y0 = yf.toInt()
                val y1 = if (y0 + 1 >= height) 0 else y0 + 1
                mixRGB(x0, y0, rgb, fx * fy)
                mixRGB(x0, y1, rgb, fx * gy)
                mixRGB(x1, y0, rgb, gx * fy)
                mixRGB(x1, y1, rgb, gx * gy)
            } else mixRGB(posMod(x, width.toFloat()), posMod(y, height.toFloat()), rgb, alpha, clamping)
            Clamping.CLAMP -> if (x in -1f..width.toFloat() && y in -1f..height.toFloat()) {
                val xf = floor(x)
                val yf = floor(y)
                val gx = (x - xf) * alpha
                val fx = alpha - gx
                val gy = (y - yf) * alpha
                val fy = alpha - gy
                val x0 = xf.toInt()
                val x1 = if (x0 + 1 >= width) 0 else x0 + 1
                val y0 = yf.toInt()
                val y1 = if (y0 + 1 >= height) 0 else y0 + 1
                if (x0 >= 0 && y0 >= 0) mixRGB(x0, y0, rgb, fx * fy)
                if (x0 >= 0 && y1 < height) mixRGB(x0, y1, rgb, fx * gy)
                if (x1 < width && y0 >= 0) mixRGB(x1, y0, rgb, gx * fy)
                if (x1 < width && y1 < height) mixRGB(x1, y1, rgb, gx * gy)
            }
            else -> throw NotImplementedError(clamping.name)
        }
    }

    fun mixRGB(x: Int, y: Int, rgb: Int, alpha: Float) {
        val i = x + y * width
        data[i] = mixARGB(data[i], rgb, alpha)
    }

    fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, color: Int, alpha: Float = color.a01()) {
        val len = max(1, max(abs(x1 - x0), abs(y1 - y0)).toInt())
        for (i in 0..len) {
            val f = i.toFloat() / len
            val lx = Maths.mix(x0, x1, f)
            val ly = Maths.mix(y0, y1, f)
            mixRGB(lx, ly, color, alpha)
        }
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