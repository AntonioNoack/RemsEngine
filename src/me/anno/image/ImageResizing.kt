package me.anno.image

import me.anno.image.raw.IntImage
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.min
import me.anno.maths.Maths.roundDiv
import me.anno.utils.Color.argb
import me.anno.utils.Color.mixARGB
import me.anno.utils.InternalAPI
import kotlin.math.roundToInt

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

        val tmp = if (dstWidth1 < srcWidth) src.downscaleX(dstWidth1)
        else src.upscaleX(dstWidth1)
        return if (dstHeight1 < srcHeight) tmp.downscaleY(dstHeight1)
        else tmp.upscaleY(dstHeight1)
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

                val x0 = xf[xd]
                val x1 = xf[xd + 1]

                val x0i = xi[xd]
                val x1i = xi[xd + 1]

                // accumulate
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f

                val xs0 = if (x0i == x1i) x1 - x0 else 1f - fract(x0)

                for (xs in x0i..x1i) {
                    val wx = if (xs == x0i) xs0 else if (xs == x1i) fract(x1) else 1f
                    val color = getRGB(xs, yd)
                    r += wx * color.shr(16).and(255)
                    g += wx * color.shr(8).and(255)
                    b += wx * color.and(255)
                    a += wx * color.ushr(24)
                }

                dst[di++] = argb(
                    (a * invArea).roundToInt(),
                    (r * invArea).roundToInt(),
                    (g * invArea).roundToInt(),
                    (b * invArea).roundToInt()
                )
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

        val sx = srcWidth.toFloat() / srcWidth
        val sy = srcHeight.toFloat() / dstHeight
        val maxHeight = srcHeight.toFloat()
        val maxHeightM1 = srcHeight - 1
        for (i in yf.indices) {
            yf[i] = min(i * sy, maxHeight)
            yi[i] = min(yf[i].toInt(), maxHeightM1)
        }

        // area is constant
        val dst = img.data
        val invArea = 1f / (sx * sy)
        var di = 0
        for (yd in 0 until dstHeight) {
            for (xd in 0 until srcWidth) {

                val y0 = yf[yd]
                val y1 = yf[yd + 1]

                val y0i = yi[yd]
                val y1i = yi[yd + 1]

                // accumulate
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f

                val ys0 = if (y0i == y1i) y1 - y0 else 1f - fract(y0)

                val c0 = getRGB(xd, y0i)
                r += ys0 * c0.shr(16).and(255)
                g += ys0 * c0.shr(8).and(255)
                b += ys0 * c0.and(255)
                a += ys0 * c0.ushr(24)

                if (y1i > y0i) {
                    for (ys in y0i + 1 until y1i) {
                        val color = getRGB(xd, ys)
                        r += color.shr(16).and(255)
                        g += color.shr(8).and(255)
                        b += color.and(255)
                        a += color.ushr(24)
                    }
                    val wy = fract(y1)
                    val c1 = getRGB(xd, y1i)
                    r += wy * c1.shr(16).and(255)
                    g += wy * c1.shr(8).and(255)
                    b += wy * c1.and(255)
                    a += wy * c1.ushr(24)
                }

                dst[di++] = argb(
                    (a * invArea).roundToInt(),
                    (r * invArea).roundToInt(),
                    (g * invArea).roundToInt(),
                    (b * invArea).roundToInt()
                )
            }
        }

        return img
    }

    private fun Image.upscaleX(dstWidth: Int): Image {
        val srcWidth = width
        if (dstWidth == srcWidth) return this
        val height = height

        val img = IntImage(dstWidth, height, hasAlphaChannel)

        // area is constant
        val dst = img.data
        var di = 0
        val dFract = srcWidth.toFloat() / dstWidth
        val maxI = srcWidth - 1
        for (yd in 0 until height) {
            var fract = 0f
            var i0 = 0
            var i1 = min(1, maxI)
            for (xd in 0 until dstWidth) {
                dst[di++] = mixARGB(getRGB(i0, yd), getRGB(i1, yd), fract)
                fract += dFract
                while (fract >= 1f) {
                    fract--
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
        val dFract = srcHeight.toFloat() / dstHeight
        val maxI = srcHeight - 1
        for (xd in 0 until width) {
            var di = xd
            var fract = 0f
            var i0 = 0
            var i1 = min(1, maxI)
            for (yd in 0 until dstHeight) {
                dst[di] = mixARGB(getRGB(xd, i0), getRGB(xd, i1), fract)
                di += width
                fract += dFract
                while (fract >= 1f) {
                    fract--
                    i0++
                    i1 = min(i1 + 1, maxI)
                }
            }
        }

        return img
    }
}