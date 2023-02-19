package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.maths.Maths
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.posMod
import me.anno.utils.Color.a01
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import kotlin.math.abs
import kotlin.math.floor

open class IntImage(
    width: Int, height: Int,
    val data: IntArray = IntArray(width * height),
    hasAlphaChannel: Boolean
) : Image(width, height, if (hasAlphaChannel) 4 else 3, hasAlphaChannel) {

    constructor(width: Int, height: Int, hasAlphaChannel: Boolean) :
            this(width, height, IntArray(width * height), hasAlphaChannel)

    fun setRGB(x: Int, y: Int, rgb: Int) {
        data[x + y * width] = rgb
    }

    fun setRGBSafely(x: Int, y: Int, rgb: Int) {
        if (x in 0 until width && y in 0 until height) {
            data[x + y * width] = rgb
        }
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
        val len = max(1, max(abs(x0 - x1), abs(y0 - y1)).toInt())
        for (i in 0..len) {
            val f = i.toFloat() / len
            val lx = Maths.mix(x0, x1, f)
            val ly = Maths.mix(y0, y1, f)
            mixRGB(lx, ly, color, alpha)
        }
    }

    override fun getRGB(index: Int): Int = data[index]

    override fun createIntImage() = this

    override fun createBufferedImage(): BufferedImage {
        val width = width
        val height = height
        val image = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
        val dataBuffer = image.raster.dataBuffer as DataBufferInt
        val dataDst = dataBuffer.data
        val dataSrc = data
        // src, dst
        System.arraycopy(dataSrc, 0, dataDst, 0, dataSrc.size)
        return image
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        // data cloning is required, because the function in Texture2D switches the red and blue channels
        if (sync && GFX.isGFXThread()) {
            if (hasAlphaChannel) texture.createBGRA(cloneData(), checkRedundancy)
            else texture.createBGR(cloneData(), checkRedundancy)
        } else {
            val data1 = Texture2D.bufferPool[data.size * 4, false, false]
            val dataI = data1.asIntBuffer()
            dataI.put(this.data).position(0)
            if (checkRedundancy) texture.checkRedundancy(dataI)
            Texture2D.switchRGB2BGR(dataI)
            // for testing, convert the data into a byte buffer
            // -> 33% faster, partially because of wrong alignment and using 25% less data effectively
            texture.createTiled(
                if (hasAlphaChannel) TargetType.UByteTarget4 else TargetType.UByteTarget3,
                TargetType.UByteTarget4,
                dataI, data1
            )
        }
    }

    override fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        val result = IntImage(w0, h0, hasAlphaChannel)
        val src = data
        val dst = result.data
        val width = width
        for (y in 0 until h0) {
            System.arraycopy(src, x0 + (y0 + y) * width, dst, y * w0, w0)
        }
        return result
    }

    fun cloneData(): IntArray {
        val clone = Texture2D.intArrayPool[data.size, false, true]
        System.arraycopy(data, 0, clone, 0, data.size)
        return clone
    }

}