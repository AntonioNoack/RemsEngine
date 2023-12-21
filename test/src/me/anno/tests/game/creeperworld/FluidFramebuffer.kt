package me.anno.tests.game.creeperworld

import me.anno.maths.Maths.min
import me.anno.tests.physics.fluid.RWState
import me.anno.utils.Color
import me.anno.utils.Color.a01
import me.anno.utils.Color.black

class FluidFramebuffer {

    val level = RWState { FloatArray(size) } // 0..1: good, 1.. high pressure
    val impulseX = RWState { FloatArray(size) }
    val impulseY = RWState { FloatArray(size) }

    fun render(dst: IntArray, color: Int) {
        val alpha = color.a01()
        val minAlpha = 1f / (255f * alpha)
        val pressure = level.read
        for (i in 0 until size) {
            val v = min(pressure[i], 1f)
            if (v > minAlpha) {
                dst[i] = mixRGB2(dst[i], color, alpha * v)
            }
        }
    }

    fun mixRGB2(a: Int, b: Int, f: Float): Int {
        return black or
                Color.mixChannel2(a, b, 16, f) or
                Color.mixChannel2(a, b, 8, f) or
                Color.mixChannel2(a, b, 0, f)
    }

}
