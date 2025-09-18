package me.anno.games.creeperworld

import me.anno.maths.MinMax.max
import kotlin.math.abs

class FluidMove : FluidWithNeighborShader {

    override fun process(fluid: FluidFramebuffer, world: CreeperWorld) {
        super.process(fluid, world)
        fluid.level.swap()
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }

    fun check(a: Double, b: Double, name: String) {
        if (a !in 0.999f * b..1.001f * b) {
            throw IllegalStateException("FluidMove isn't conservative, $a -> $b, $name")
        }
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: CreeperWorld) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val hardness = world.hardness

        val w = world.w
        for (i in i0 until i1) {
            // based on transfer, transfer fluid and velocity
            if (!isSolid(hardness[i])) {
                var sumH = 0f
                var sumVX = 0f
                var sumVY = 0f
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val j = i + dx + dy * w
                        val level = srcH[j]
                        if (level > 0f) {
                            // calculate flow from that cell into this cell
                            val flowX = 1f - abs(srcVX[j] / level + dx) // [0, +1]
                            val flowY = 1f - abs(srcVY[j] / level + dy) // [0, +1]
                            val flow = max(flowX, 0f) * max(flowY, 0f) // [0, +1]
                            sumH += level * flow
                            sumVX += srcVX[j] * flow
                            sumVY += srcVY[j] * flow
                        }
                    }
                }
                dstH[i] = sumH
                dstVX[i] = sumVX
                dstVY[i] = sumVY
            }
        }
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: CreeperWorld) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val hardness = world.hardness
        if (!isSolid(hardness[i])) {

            val dstH = fluid.level.write
            val dstVX = fluid.impulseX.write
            val dstVY = fluid.impulseY.write

            var sumH = 0f
            var sumVX = 0f
            var sumVY = 0f
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val xi = x + dx
                    val yi = y + dy
                    if (xi !in 0 until world.w || yi !in 0 until world.h) continue
                    val j = xi + yi * world.w
                    val level = srcH[j]
                    if (level > 0f) {
                        // calculate flow from that cell into this cell
                        val flowX = max(1f - abs(srcVX[j] / level + dx), 0f) // [0, +1]
                        val flowY = max(1f - abs(srcVY[j] / level + dy), 0f) // [0, +1]
                        val flow = flowX * flowY // [0, +1]
                        sumH += level * flow
                        sumVX += srcVX[j] * flow
                        sumVY += srcVY[j] * flow
                    }
                }
            }
            dstH[i] = sumH
            dstVX[i] = sumVX
            dstVY[i] = sumVY
        }
    }
}
