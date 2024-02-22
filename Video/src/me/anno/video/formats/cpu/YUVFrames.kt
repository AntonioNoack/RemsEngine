package me.anno.video.formats.cpu

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.types.InputStreams.readNBytes2
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

    fun int00(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        return data[bj].toInt().and(255)
    }

    fun int10(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        return mix(data[bj], data[bj + 1])
    }

    fun int01(xi: Int, yi: Int, w2: Int, data: ByteBuffer): Int {
        val xj = xi shr 1
        val yj = yi shr 1
        val bj = xj + yj * w2
        return mix(data[bj], data[bj + w2])
    }

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

        val yData = input.readNBytes2(s0, Texture2D.bufferPool)
        val uData = input.readNBytes2(s0, Texture2D.bufferPool)
        val vData = input.readNBytes2(s0, Texture2D.bufferPool)

        val data = IntArray(w * h) {
            yuv2rgb(yData[it], uData[it], vData[it])
        }

        Texture2D.bufferPool.returnBuffer(yData)
        Texture2D.bufferPool.returnBuffer(uData)
        Texture2D.bufferPool.returnBuffer(vData)

        return IntImage(w, h, data, false)
    }

    fun loadI420Frame(w: Int, h: Int, input: InputStream): Image {

        val s0 = w * h

        val yData = input.readNBytes2(s0, Texture2D.bufferPool)

        // this is correct, confirmed by example
        val w2 = (w + 1) / 2
        val h2 = (h + 1) / 2

        val s1 = w2 * h2
        val uData = input.readNBytes2(s1, Texture2D.bufferPool)
        val vData = input.readNBytes2(s1, Texture2D.bufferPool)

        val data = IntArray(w * h)
        /* {
            // this is hell for branch prediction -> do better, section by section
            // even if we have a bit of strided access
            // in my test with an image and 10k runs,
            // my improved method was 1.6x faster... so not much, a little
            val xi = it % w
            val yi = it / w
            yuv2rgb(
                yData[it],
                interpolate(xi, yi, w2, uData),
                interpolate(xi, yi, w2, vData)
            )
        }*/

        val wx = w + w.and(1) - 1 // same if odd, -1 else
        val hx = h + h.and(1) - 1

        for (yi in 0 until hx step 2) {
            var it = yi * w
            for (xi in 0 until wx step 2) {
                data[it] = yuv2rgb(
                    yData[it],
                    int00(xi, yi, w2, uData),
                    int00(xi, yi, w2, vData)
                )
                it += 2
            }
            it = 1 + yi * w
            for (xi in 1 until wx step 2) {
                data[it] = yuv2rgb(
                    yData[it],
                    int10(xi, yi, w2, uData),
                    int10(xi, yi, w2, vData)
                )
                it += 2
            }
        }

        for (yi in 1 until hx step 2) {
            var it = yi * w
            for (xi in 0 until wx step 2) {
                data[it] = yuv2rgb(
                    yData[it],
                    int01(xi, yi, w2, uData),
                    int01(xi, yi, w2, vData)
                )
                it += 2
            }
            it = 1 + yi * w
            for (xi in 1 until wx step 2) {
                data[it] = yuv2rgb(
                    yData[it],
                    int11(xi, yi, w2, uData),
                    int11(xi, yi, w2, vData)
                )
                it += 2
            }
        }

        if (h != hx) {
            // last stripe without interpolation
            // todo interpolation 90° to that direction (except last pixel)
            var it = hx * w
            for (xi in 0 until w) {
                data[it] = yuv2rgb(
                    yData[it],
                    int00(xi, hx, w2, uData),
                    int00(xi, hx, w2, vData)
                )
                it++
            }
        }

        if (w != wx) {
            // last stripe without interpolation
            // todo interpolation 90° to that direction (except last pixel)
            for (yi in 0 until h) {
                val it = wx + yi * w
                data[it] = yuv2rgb(
                    yData[it],
                    int00(wx, yi, w2, uData),
                    int00(wx, yi, w2, vData)
                )
            }
        }

        Texture2D.bufferPool.returnBuffer(yData)
        Texture2D.bufferPool.returnBuffer(uData)
        Texture2D.bufferPool.returnBuffer(vData)

        return IntImage(w, h, data, false)
    }
}