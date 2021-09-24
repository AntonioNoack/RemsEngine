package me.anno.video.formats.cpu

import me.anno.image.Image
import me.anno.utils.Color.rgba
import java.io.InputStream

abstract class CPUFrame {

    abstract fun load(w: Int, h: Int, input: InputStream): Image

    fun yuv2rgb(y: Byte, u: Byte, v: Byte): Int {
        return yuv2rgb(
            y.toInt().and(255) / 255f,
            u.toInt().and(255) / 255f,
            v.toInt().and(255) / 255f
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

}