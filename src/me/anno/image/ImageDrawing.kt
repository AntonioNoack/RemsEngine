package me.anno.image

import me.anno.gpu.texture.Clamping
import me.anno.maths.Maths
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.posMod
import me.anno.utils.Color.a01
import me.anno.utils.Color.mixARGB2
import kotlin.math.abs
import kotlin.math.floor

object ImageDrawing {

    fun Image.mixRGB(x: Float, y: Float, rgb: Int, alpha: Float = rgb.a01(), clamping: Clamping = Clamping.REPEAT) {
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
            } else {
                val nx = posMod(x, width.toFloat())
                val ny = posMod(y, height.toFloat())
                mixRGB(nx, ny, rgb, alpha, clamping)
            }
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

    fun Image.mixRGB(x: Int, y: Int, rgb: Int, alpha: Float) {
        setRGB(x, y, mixARGB2(getRGB(x, y), rgb, alpha))
    }

    fun Image.drawLine(x0: Float, y0: Float, x1: Float, y1: Float, color: Int, alpha: Float = color.a01()) {
        val len = max(1, max(abs(x1 - x0), abs(y1 - y0)).toInt())
        for (i in 0..len) {
            val f = i.toFloat() / len
            val lx = Maths.mix(x0, x1, f)
            val ly = Maths.mix(y0, y1, f)
            mixRGB(lx, ly, color, alpha)
        }
    }
}