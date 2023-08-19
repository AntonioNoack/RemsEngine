package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import java.io.InputStream

abstract class CPUFrame {

    abstract fun load(w: Int, h: Int, input: InputStream): Image

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

    fun yuv2rgb(y: Byte, u: Float, v: Float): Int {
        return yuv2rgb(y.toInt().and(255) / 255f, u, v)
    }

    fun yuv2rgb(y: Float, u: Float, v: Float): Int {
        // equal to yuv2rgb(y.toInt(),u.toInt(),v.toInt())?
        val y2 = 1.164f * (y - 16f / 255f)
        val u2 = u - 0.5f
        val v2 = v - 0.5f
        val r = y2 + 1.596f * v2
        val g = y2 - 0.392f * u2 - 0.813f * v2
        val b = y2 + 2.017f * u2
        return rgba(r, g, b, 1f)
    }

    fun yuv2rgb(y: Int, u: Int, v: Int, a: Int): Int {
        val r = y + (+91881 * v - 11698176).shr(16)
        val g = y + (-22544 * u - 46793 * v + 8840479).shr(16)
        val b = y + (+116130 * u - 14823260).shr(16)
        return rgba(r, g, b, a)
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
        return rgba(y, u, v, a)
    }

    fun rgb2yuv(rgb: Int): Int {
        val r = rgb.r()
        val g = rgb.g()
        val b = rgb.b()
        val a = rgb.a()
        return rgb2yuv(r, g, b, a)
    }
}