package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.utils.Color.rgba
import java.io.InputStream

abstract class CPUFrame {

    abstract fun load(w: Int, h: Int, input: InputStream): Image

    fun yuv2rgb(y: Byte, u: Int, v: Int): Int {
        return yuv2rgb(y.toInt().and(255), u, v)
    }

    fun yuv2rgb(y: Byte, u: Byte, v: Byte): Int {
        return yuv2rgb(
            y.toInt().and(255),
            u.toInt().and(255),
            v.toInt().and(255)
        )
    }

    fun yuv2rgb(y: Byte, u: Float, v: Float): Int {
        return yuv2rgb(y.toInt().and(255) / 255f, u, v)
    }

    fun yuv2rgb(y: Float, u: Float, v: Float): Int {
        val y2 = 1.164f * (y - 16f / 255f)
        val u2 = u - 0.5f
        val v2 = v - 0.5f
        val r = y2 + 1.596f * v2
        val g = y2 - 0.392f * u2 - 0.813f * v2
        val b = y2 + 2.017f * u2
        return rgba(r, g, b, 1f)
    }

    fun yuv2rgb(y: Int, u: Int, v: Int): Int {
        // 1024 * all values
        val y2 = 1192 * (y - 16) // [0,255] * 1024
        val u2 = u.shl(1) - 255 // [0,255] * 2
        val v2 = v.shl(1) - 255 // [0,255] * 2
        var r = (y2 + 817 * v2).shr(10)
        var g = (y2 - 201 * u2 - 416 * v2).shr(10)
        var b = (y2 + 1033 * u2).shr(10)
        if (r < 0) r = 0 else if (r > 255) r = 255
        if (g < 0) g = 0 else if (g > 255) g = 255
        if (b < 0) b = 0 else if (b > 255) b = 255
        return r.shl(16) or g.shl(8) or b or (255.shl(24))
    }

}