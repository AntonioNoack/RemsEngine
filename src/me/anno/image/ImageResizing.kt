package me.anno.image

import me.anno.image.raw.IntImage
import me.anno.maths.Maths.fract
import me.anno.maths.MinMax.min
import me.anno.maths.Maths.roundDiv
import me.anno.utils.Color.a
import me.anno.utils.Color.argb
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.InternalAPI
import me.anno.utils.types.Floats.roundToIntOr

@InternalAPI
object ImageResizing {

    @InternalAPI
    fun resized(src: Image, dstWidth: Int, dstHeight: Int, allowUpscaling: Boolean): Image {

        // todo add optional padding, if the aspect ratio isn't fitting

        var dstWidth1 = dstWidth
        var dstHeight1 = dstHeight

        val srcWidth = src.width
        val srcHeight = src.height

        if (!allowUpscaling) {
            if (dstWidth1 > srcWidth) {
                dstHeight1 = roundDiv(dstHeight1 * srcWidth, dstWidth1)
                dstWidth1 = srcWidth
            }

            if (dstHeight1 > srcHeight) {
                dstWidth1 = roundDiv(dstWidth1 * srcHeight, dstHeight1)
                dstHeight1 = srcHeight
            }
        }

        if (dstWidth1 == srcWidth && dstHeight1 == srcHeight) {
            return src
        }

        val tmp = if (dstWidth1 < srcWidth) src.downscaleX(dstWidth1) else src.upscaleX(dstWidth1)
        return if (dstHeight1 < srcHeight) tmp.downscaleY(dstHeight1) else tmp.upscaleY(dstHeight1)
    }

    private fun Image.downscaleX(dstWidth: Int): Image {

        val dstHeight = height
        val srcWidth = width

        if (dstWidth == srcWidth) return this

        val img = IntImage(dstWidth, dstHeight, hasAlphaChannel)

        val xf = FloatArray(dstWidth + 1)
        val xi = IntArray(dstWidth + 1)

        val sx = srcWidth.toFloat() / dstWidth
        val maxWidth = srcWidth.toFloat()
        val maxWidthM1 = srcWidth - 1

        for (i in xf.indices) {
            xf[i] = min(i * sx, maxWidth)
            xi[i] = min(xf[i].toInt(), maxWidthM1)
        }

        // area is constant
        val dst = img.data
        val invArea = 1f / sx
        var di = 0
        for (yd in 0 until dstHeight) {
            for (xd in 0 until dstWidth) {

                val x0i = xi[xd]
                val x1i = xi[xd + 1]

                if (x0i == x1i) {
                    dst[di++] = getRGB(x1i, yd)
                } else {

                    val x0 = xf[xd]
                    val x1 = xf[xd + 1]

                    // accumulate
                    val w0 = 1f - fract(x0)
                    val c0 = getRGB(x1i, yd)
                    var r = w0 * c0.r()
                    var g = w0 * c0.g()
                    var b = w0 * c0.b()
                    var a = w0 * c0.a()

                    for (xs in x0i + 1 until x1i) {
                        val color = getRGB(xs, yd)
                        r += color.r()
                        g += color.g()
                        b += color.b()
                        a += color.a()
                    }

                    val w1 = x1 - x1i
                    val c1 = getRGB(x1i, yd)
                    r += w1 * c1.r()
                    g += w1 * c1.g()
                    b += w1 * c1.b()
                    a += w1 * c1.a()

                    dst[di++] = joinColor(a, r, g, b, invArea)
                }
            }
        }

        return img
    }

    private fun Image.downscaleY(dstHeight: Int): Image {

        val srcWidth = width
        val srcHeight = height

        if (dstHeight == srcHeight) return this

        val img = IntImage(srcWidth, dstHeight, hasAlphaChannel)

        val yf = FloatArray(dstHeight + 1)
        val yi = IntArray(dstHeight + 1)

        val sy = srcHeight.toFloat() / dstHeight
        val maxHeight = srcHeight.toFloat()
        val maxHeightM1 = srcHeight - 1
        for (i in yf.indices) {
            yf[i] = min(i * sy, maxHeight)
            yi[i] = min(yf[i].toInt(), maxHeightM1)
        }

        // area is constant
        val dst = img.data
        val invArea = 1f / sy
        var di = 0
        for (yd in 0 until dstHeight) {
            for (xd in 0 until srcWidth) {

                val y0i = yi[yd]
                val y1i = yi[yd + 1]

                if (y0i == y1i) {
                    dst[di++] = getRGB(xd, y0i)
                } else {

                    val y0 = yf[yd]
                    val y1 = yf[yd + 1]

                    // accumulate
                    val ys0 = 1f - fract(y0)
                    val c0 = getRGB(xd, y0i)
                    var r = ys0 * c0.shr(16).and(255)
                    var g = ys0 * c0.shr(8).and(255)
                    var b = ys0 * c0.and(255)
                    var a = ys0 * c0.ushr(24)

                    for (ys in y0i + 1 until y1i) {
                        val color = getRGB(xd, ys)
                        r += color.shr(16).and(255)
                        g += color.shr(8).and(255)
                        b += color.and(255)
                        a += color.ushr(24)
                    }
                    val wy = y1 - y1i
                    val c1 = getRGB(xd, y1i)
                    r += wy * c1.shr(16).and(255)
                    g += wy * c1.shr(8).and(255)
                    b += wy * c1.and(255)
                    a += wy * c1.ushr(24)

                    dst[di++] = joinColor(a, r, g, b, invArea)
                }
            }
        }

        return img
    }

    private fun joinColor(a: Float, r: Float, g: Float, b: Float, invArea: Float): Int {
        return argb(
            (a * invArea).roundToIntOr(),
            (r * invArea).roundToIntOr(),
            (g * invArea).roundToIntOr(),
            (b * invArea).roundToIntOr()
        )
    }

    private fun Image.upscaleX(dstWidth: Int): Image {
        val srcWidth = width
        if (dstWidth == srcWidth) return this
        val height = height

        val img = IntImage(dstWidth, height, hasAlphaChannel)

        // area is constant
        val dst = img.data
        var di = 0
        val maxI = srcWidth - 1
        for (yd in 0 until height) {
            var fract = 0
            var i0 = 0
            var i1 = min(1, maxI)
            for (xd in 0 until dstWidth) {
                dst[di++] = mixARGBi(getRGB(i0, yd), getRGB(i1, yd), fract, dstWidth)
                fract += srcWidth
                while (fract >= dstWidth) {
                    fract -= dstWidth
                    i0++
                    i1 = min(i1 + 1, maxI)
                }
            }
        }

        return img
    }

    private fun Image.upscaleY(dstHeight: Int): Image {
        val srcHeight = height
        if (dstHeight == srcHeight) return this
        val width = width

        val img = IntImage(width, dstHeight, hasAlphaChannel)

        // area is constant
        val dst = img.data
        val maxI = srcHeight - 1
        for (xd in 0 until width) {
            var di = xd
            var fract = 0
            var i0 = 0
            var i1 = min(1, maxI)
            for (yd in 0 until dstHeight) {
                dst[di] = mixARGBi(getRGB(xd, i0), getRGB(xd, i1), fract, dstHeight)
                di += width
                fract += srcHeight
                while (fract >= dstHeight) {
                    fract -= dstHeight
                    i0++
                    i1 = min(i1 + 1, maxI)
                }
            }
        }

        return img
    }

    @JvmStatic
    private fun mixChannelI(a: Int, b: Int, shift: Int, f: Int, g: Int, div: Int): Int {
        val ai = (a ushr shift) and 0xff
        val bi = (b ushr shift) and 0xff
        val ci = (ai * g + bi * f) / div
        return ci shl shift
    }

    @JvmStatic
    private fun mixARGBi(a: Int, b: Int, f: Int, div: Int): Int {
        val g = div - f
        return mixChannelI(a, b, 24, f, g, div) or
                mixChannelI(a, b, 16, f, g, div) or
                mixChannelI(a, b, 8, f, g, div) or
                mixChannelI(a, b, 0, f, g, div)
    }
}