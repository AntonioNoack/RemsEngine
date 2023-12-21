package me.anno.tests.game.creeperworld

import kotlin.math.exp
import kotlin.math.min

/**
 * this is currently not 100% thread-safe, different runs can have different results
 * */
class FluidDissolveRock(
    val rockDissolvingSpeed: Float,
    val fluidDilutingRate: Float,
    val result: FluidFramebuffer
) : FluidWithNeighborShader {

    fun set(old: Float, new: Float, level: FloatArray, vx: FloatArray, vy: FloatArray, i: Int) {
        level[i] = new
        val ratio = new / old
        vx[i] *= ratio
        vy[i] *= ratio
    }

    fun change(
        src: FloatArray, vx: FloatArray, vy: FloatArray,
        dst: FloatArray, i: Int
    ) {
        val srcI = src[i]
        val diluted = srcI * fluidDilutingRate
        val factor = (1f - fluidDilutingRate)
        src[i] = srcI - diluted
        vx[i] *= factor
        vy[i] *= factor
        dst[i] += diluted
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: CreeperWorld) {

        val hardness = world.hardness
        val dissolved = world.dissolved

        val srcH = fluid.level.read
        val srcX = fluid.impulseX.read
        val srcY = fluid.impulseY.read
        val dstH = result.level.read
        val w = world.w
        val y = i0 / w
        for (i in i0 until i1) {
            if (world.rockTypes[i] != 0) {
                val fluidAround = srcH[i - 1] + srcH[i + 1] + srcH[i + w] + srcH[i - w]
                if (fluidAround > 0f) {
                    val delta = min(rockDissolvingSpeed * fluidAround * exp(-10f * hardness[i]), 1f - dissolved[i])
                    dstH[i] += delta
                    // todo maybe make the ratio not 1:1, but dependant on the hardness?
                    // add the fluid, there the acid is, not inside the rock
                    //  remove a bit of the original fluid... we need locks...
                    change(srcH, srcX, srcY, dstH, i - 1)
                    change(srcH, srcX, srcY, dstH, i + 1)
                    change(srcH, srcX, srcY, dstH, i - w)
                    change(srcH, srcX, srcY, dstH, i + w)
                    dissolved[i] += delta
                    if (dissolved[i] >= 1f) {
                        // dissolved rock
                        dissolved[i] = 0f
                        world.setBlock(i, null)
                    }
                }
            }
        }
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: CreeperWorld) {
        // edge cannot be dissolved
    }
}