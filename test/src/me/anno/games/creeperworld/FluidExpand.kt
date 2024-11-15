package me.anno.games.creeperworld

import kotlin.math.min

class FluidExpand : FluidShader {
    override fun process(fluid: FluidFramebuffer, world: CreeperWorld) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val hardness = world.hardness

        val w = world.w
        val h = world.h
        worker.processBalanced(0, w, 8) { x0, x1 ->
            for (x in x0 until x1) {
                var y = h - 1
                var i = x + y * w
                while (y > 0) {
                    if (srcH[i] > 0f) {
                        var remainingH = 0f
                        var remVX = 0f
                        var remVY = 0f
                        val yMax = y
                        do {
                            remainingH += srcH[i]
                            remVX += srcVX[i]
                            remVY += srcVY[i]
                            val level = min(remainingH, 1f)
                            val portion = level / remainingH
                            dstH[i] = level
                            dstVX[i] = remVX * portion
                            dstVY[i] = if (remainingH > 1f) {
                                // we're moving things up, so they must not count as flowing down
                                min(remVY * portion, 0f)
                            } else {
                                remVY * portion
                            }
                            remainingH -= level
                            remVX *= (1f - portion)
                            remVY *= (1f - portion)
                            y--
                            i -= w
                        } while (remainingH > 0f && y >= 0 && !isSolid(hardness[i]))
                        if (remainingH > 0f) {
                            // distribute remaining fluid
                            val portion = 1f / (yMax - y)
                            i = x + yMax * w
                            for (yi in y until yMax) {
                                dstH[i] += remainingH * portion
                                dstVX[i] += remVX * portion
                                dstVY[i] += remVY * portion
                                i -= w
                            }
                        }
                    } else {
                        y--
                        i -= w
                    }
                }
            }
        }
        fluid.level.swap()
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }
}
