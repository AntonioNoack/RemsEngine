package me.anno.tests.game.creeperworld

import me.anno.maths.Maths.min
import me.anno.tests.physics.fluid.RWState
import me.anno.utils.Color.a01
import me.anno.utils.Color.mixARGB

class FluidFramebuffer {

    val level = RWState { FloatArray(size) } // 0..1: good, 1.. high pressure
    val impulseX = RWState { FloatArray(size) }
    val impulseY = RWState { FloatArray(size) }

    fun render(dst: IntArray, color: Int) {
        val alpha = color.a01()
        val pressure = level.read
        for (i in 0 until size) {
            val v = min(pressure[i], 1f)
            if (v > 0f) {
                dst[i] = mixARGB(dst[i], color, alpha * v)
            }
        }
    }
}
