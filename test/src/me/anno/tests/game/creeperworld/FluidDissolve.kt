package me.anno.tests.game.creeperworld

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FluidDissolve(val pos: FluidFramebuffer, val neg: FluidFramebuffer) : FluidShader {

    fun set(old: Float, new: Float, level: FloatArray, vx: FloatArray, vy: FloatArray, i: Int) {
        level[i] = new
        val ratio = new / old
        vx[i] *= ratio
        vy[i] *= ratio
    }

    override fun process(fluid: FluidFramebuffer, world: CreeperWorld) {
        val level = fluid.level.read
        val posH = pos.level.read
        val negH = neg.level.read
        val posX = pos.impulseX.read
        val posY = pos.impulseY.read
        val negX = neg.impulseX.read
        val negY = neg.impulseY.read
        worker.processBalanced(0, world.size, 64) { i0, i1 ->
            for (i in i0 until i1) {
                val pi = posH[i]
                val ni = negH[i]
                val minI = min(pi, ni)
                if (minI > 0f) {
                    val neutral = pi - ni
                    level[i] += (abs(pi) + abs(ni) - abs(neutral))
                    set(pi, max(0f, +neutral), posH, posX, posY, i)
                    set(ni, max(0f, -neutral), negH, negX, negY, i)
                }
            }
        }
    }
}