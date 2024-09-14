package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.pooling.Pools
import java.io.InputStream
import java.nio.ByteBuffer

object YUVFrames {

    fun yuv2rgb(y: Byte, u: Int, v: Int): Int {
        return yuv2rgb(y.toInt().and(255), u, v, 255)
    }

    fun yuv2rgb(y: Byte, u: Byte, v: Byte): Int {
        return yuv2rgb(
            y.toInt().and(255),
            u.toInt().and(255),
            v.toInt().and(255),
            255
        )
    }

    fun yuv2rgb(y: Int, u: Int, v: Int, a: Int): Int {
        val r = y + (+91881 * v - 11698176).shr(16)
        val g = y + (-22544 * u - 46793 * v + 8840479).shr(16)
        val b = y + (+116130 * u - 14823260).shr(16)
        return Color.rgba(r, g, b, a)
    }

    fun yuv2rgb(yuv: Int): Int {
        val y = yuv.r()
        val u = yuv.g()
        val v = yuv.b()
        val a = yuv.a()
        return yuv2rgb(y, u, v, a)
    }

    fun rgb2yuv(r: Int, g: Int, b: Int, a: Int): Int {
        val y = (+19595 * r + 38470 * g + 7471 * b).shr(16)
        val u = (-11076 * r - 21692 * g + 32768 * b + 8355840).shr(16)
        val v = (+32768 * r - 27460 * g - 5308 * b + 8355840).shr(16)
        return Color.rgba(y, u, v, a)
    }

    fun rgb2yuv(rgb: Int): Int {
        val r = rgb.r()
        val g = rgb.g()
        val b = rgb.b()
        val a = rgb.a()
        return rgb2yuv(r, g, b, a)
    }

    fun mix(a: Byte, b: Byte): Int {
        return (a.toInt().and(255) + b.toInt().and(255))
            .shr(1)
    }

    /**
     * loads the value from a 1x1 field
     * */
    fun int00(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        return data[bj].toInt().and(255)
    }

    /**
     * loads the average value from a 2x1 field
     * */
    fun int10(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        return mix(data[bj], data[bj + 1])
    }

    /**
     * loads the average value from a 1x2 field
     * */
    fun int01(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        return mix(data[bj], data[bj + w2])
    }

    /**
     * loads the average value from a 2x2 field
     * */
    fun int11(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        val a = data[bj].toInt().and(255)
        val b = data[bj + 1].toInt().and(255)
        val c = data[bj + w2].toInt().and(255)
        val d = data[bj + w2 + 1].toInt().and(255)
        return (a + b + c + d).shr(2)
    }

    // this seems to work, and to be correct
    fun loadI444Frame(w: Int, h: Int, input: InputStream): Image {

        val s0 = w * h

        val yData = input.readNBytes2(s0, Pools.byteBufferPool)
        val uData = input.readNBytes2(s0, Pools.byteBufferPool)
        val vData = input.readNBytes2(s0, Pools.byteBufferPool)

        val data = IntArray(w * h) {
            yuv2rgb(yData[it], uData[it], vData[it])
        }

        Pools.byteBufferPool.returnBuffer(yData)
        Pools.byteBufferPool.returnBuffer(uData)
        Pools.byteBufferPool.returnBuffer(vData)

        return IntImage(w, h, data, false)
    }

    fun loadI420Frame(w: Int, h: Int, input: InputStream): Image {

        val s0 = w * h

        val yData = input.readNBytes2(s0, Pools.byteBufferPool)

        // this is correct, confirmed by example
        val w2 = (w + 1) shr 1
        val h2 = (h + 1) shr 1

        val s1 = w2 * h2
        val uData = input.readNBytes2(s1, Pools.byteBufferPool)
        val vData = input.readNBytes2(s1, Pools.byteBufferPool)

        val result = IntArray(w * h)
        val hx = h + h.and(1) - 1 // same if odd, 1 less else

        for (yi in 0 until hx step 2) {
            xAxisInterpolation(result, w, w2, yi, yData, uData, vData)
            xyAxisInterpolation(result, w, w2, yi + 1, yData, uData, vData)
        }

        if (h != hx) {
            // last stripe with half interpolation -> only interpolated on x-axis
            xAxisInterpolation(result, w, w2, hx, yData, uData, vData)
        }

        Pools.byteBufferPool.returnBuffer(yData)
        Pools.byteBufferPool.returnBuffer(uData)
        Pools.byteBufferPool.returnBuffer(vData)

        return IntImage(w, h, result, false)
    }

    private fun xAxisInterpolation(
        result: IntArray, w: Int, w2: Int, yi: Int,
        yData: ByteBuffer, uData: ByteBuffer, vData: ByteBuffer
    ) {
        var idx = yi * w
        var xi = 0
        while (xi < w) {
            result[idx] = yuv2rgb( // even columns
                yData[idx],
                int00(xi, yi, w2, uData),
                int00(xi, yi, w2, vData)
            )
            idx++
            if (++xi >= w) break
            result[idx] = yuv2rgb( // true in 99% of cases
                yData[idx],
                int10(xi, yi, w2, uData),
                int10(xi, yi, w2, vData)
            )
            idx++
            xi++
        }
    }

    private fun xyAxisInterpolation(
        result: IntArray, w: Int, w2: Int, yi: Int,
        yData: ByteBuffer, uData: ByteBuffer, vData: ByteBuffer
    ) {
        var idx = yi * w
        var xi = 0
        while (xi < w) {
            result[idx] = yuv2rgb( // even columns
                yData[idx++],
                int01(xi, yi, w2, uData),
                int01(xi, yi, w2, vData)
            )
            if (++xi >= w) break
            result[idx] = yuv2rgb( // odd columns, true in 99% of cases
                yData[idx],
                int11(xi, yi, w2, uData),
                int11(xi, yi, w2, vData)
            )
            idx++
            xi++
        }
    }
}