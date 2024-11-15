package me.anno.games.creeperworld

import me.anno.maths.Maths.min
import me.anno.tests.physics.fluid.RWState
import me.anno.utils.Color
import me.anno.utils.Color.a01
import me.anno.utils.Color.black

class FluidFramebuffer(val world: CreeperWorld, val layer: FluidLayer) {

    val level = RWState { FloatArray(world.size) } // 0..1: good, 1.. high pressure
    val impulseX = RWState { FloatArray(world.size) }
    val impulseY = RWState { FloatArray(world.size) }

    fun render(dst: IntArray, color: Int) {
        val alpha = color.a01()
        val minAlpha = 1f / (255f * alpha)
        val pressure = level.read
        for (i in dst.indices) {
            val v = min(pressure[i], 1f)
            if (v > minAlpha) {
                dst[i] = mixRGB2(dst[i], color, alpha * v)
            }
        }
    }
companion object {
    fun mixRGB2(a: Int, b: Int, f: Float): Int {
        return black or
                Color.mixChannel2(a, b, 16, f) or
                Color.mixChannel2(a, b, 8, f) or
                Color.mixChannel2(a, b, 0, f)
    }
}
}
