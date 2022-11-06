package me.anno.image

import me.anno.cache.ICacheData
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.roundDiv
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.OutputStream
import javax.imageio.ImageIO
import kotlin.math.floor

abstract class Image(
    open var width: Int,
    open var height: Int,
    var numChannels: Int,
    var hasAlphaChannel: Boolean
) : ICacheData {

    open fun getIndex(x: Int, y: Int): Int {
        val xi = clamp(x, 0, width - 1)
        val yi = clamp(y, 0, height - 1)
        return xi + yi * width
    }

    open fun createIntImage(): IntImage {
        val width = width
        val height = height
        val data = IntArray(width * height)
        val image = IntImage(width, height, data, hasAlphaChannel)
        var i = 0
        val size = width * height
        while (i < size) {
            data[i] = getRGB(i)
            i++
        }
        return image
    }

    open fun createBufferedImage(): BufferedImage {
        val width = width
        val height = height
        val image = BufferedImage(width, height, if (hasAlphaChannel) 2 else 1)
        val dataBuffer = image.raster.dataBuffer as DataBufferInt
        val data = dataBuffer.data
        var i = 0
        val size = width * height
        while (i < size) {
            data[i] = getRGB(i)
            i++
        }
        return image
    }

    abstract fun getRGB(index: Int): Int

    fun getValueAt(x: Float, y: Float, shift: Int): Float {
        val xf = floor(x)
        val yf = floor(y)
        val xi = xf.toInt()
        val yi = yf.toInt()
        val fx = x - xf
        val gx = 1f - fx
        val fy = y - yf
        val gy = 1f - fy
        var c00: Int
        var c01: Int
        var c10: Int
        var c11: Int
        val width = width
        if (xi >= 0 && yi >= 0 && xi < width - 1 && yi < height - 1) {
            // safe
            val index = xi + yi * width
            c00 = getRGB(index)
            c01 = getRGB(index + width)
            c10 = getRGB(index + 1)
            c11 = getRGB(index + 1 + width)
        } else {
            // border
            c00 = getSafeRGB(xi, yi)
            c01 = getSafeRGB(xi, yi + 1)
            c10 = getSafeRGB(xi + 1, yi)
            c11 = getSafeRGB(xi + 1, yi + 1)
        }
        c00 = c00 shr shift and 255
        c01 = c01 shr shift and 255
        c10 = c10 shr shift and 255
        c11 = c11 shr shift and 255
        val r0 = c00 * gy + fy * c01
        val r1 = c10 * gy + fy * c11
        return r0 * gx + fx * r1
    }

    fun getRGB(x: Int, y: Int): Int {
        return getRGB(x + y * width)
    }

    fun getSafeRGB(x: Int, y: Int): Int {
        return getRGB(getIndex(x, y))
    }

    open fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        texture.create(createIntImage(), sync = sync, checkRedundancy = true)
    }

    open fun createBufferedImage(dstWidth: Int, dstHeight: Int): BufferedImage {
        return resized(dstWidth, dstHeight).createBufferedImage()
    }

    open fun resized(dstWidth: Int, dstHeight: Int): Image {

        var dstWidth1 = dstWidth
        var dstHeight1 = dstHeight

        val srcWidth = width
        val srcHeight = height

        if (dstWidth1 > srcWidth) {
            dstHeight1 = roundDiv(dstHeight1 * srcWidth, dstWidth1)
            dstWidth1 = srcWidth
        }

        if (dstHeight1 > srcHeight) {
            dstWidth1 = roundDiv(dstWidth1 * srcHeight, dstHeight1)
            dstHeight1 = srcHeight
        }

        if (dstWidth1 == srcWidth && dstHeight1 == srcHeight) {
            return this
        }

        val img = IntImage(dstWidth1, dstHeight1, hasAlphaChannel)
        val buffer = img.data
        var srcY0 = 0
        var dstY = 0
        var dstIndex = 0
        while (dstY < dstHeight1) {
            val srcY1 = (dstY + 1) * srcHeight / dstHeight1
            val srcDY = srcY1 - srcY0
            val srcIndexY0 = srcY0 * srcWidth
            var dstX = 0
            while (dstX < dstWidth1) {
                val srcX0 = dstX * srcWidth / dstWidth1
                val srcX1 = (dstX + 1) * srcWidth / dstWidth1
                // we could use better interpolation, but it shouldn't really matter
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var srcIndexYI = srcIndexY0
                // use interpolation
                for (y0 in srcY0 until srcY1) {
                    val startIndex = srcX0 + srcIndexYI
                    val endIndex = srcX1 + srcIndexYI
                    for (i in startIndex until endIndex) {
                        val color = getRGB(i)
                        a += color shr 24 and 255
                        r += color shr 16 and 255
                        g += color shr 8 and 255
                        b += color and 255
                    }
                    srcIndexYI += srcWidth
                }
                val count = (srcX1 - srcX0) * srcDY
                if (count > 1) {
                    a /= count
                    r /= count
                    g /= count
                    b /= count
                }
                buffer[dstIndex] = argb(a, r, g, b)
                dstX++
                dstIndex++
            }
            srcY0 = srcY1
            dstY++
        }
        return img
    }

    open fun write(dst: FileReference) {
        val format = dst.lcExtension
        dst.outputStream().use { out -> write(out, format) }
    }

    fun write(dst: OutputStream, format: String) {
        val image = createBufferedImage()
        ImageIO.write(image, format, dst)
    }

    override fun destroy() {}

    /**
     * for debugging and easier seeing pixels
     * */
    fun scaleUp(sx: Int, sy: Int): Image {
        return object : Image(width * sx, height * sy, numChannels, hasAlphaChannel) {
            override fun getRGB(index: Int): Int {
                val x = (index % this.width) / sx
                val y = (index / this.width) / sy
                return this@Image.getRGB(x, y)
            }
        }
    }

    companion object {

        fun argb(a: Byte, r: Byte, g: Byte, b: Byte): Int {
            return argb(
                a.toUInt().toInt(), r.toUInt().toInt(),
                g.toUInt().toInt(), b.toUInt().toInt()
            )
        }

        fun argb(a: Int, r: Int, g: Int, b: Int): Int {
            return (a shl 24) + (r shl 16) + (g shl 8) + b
        }

    }
}