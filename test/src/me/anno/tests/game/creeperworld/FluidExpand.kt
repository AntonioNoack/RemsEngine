package me.anno.tests.game.creeperworld

import kotlin.math.min

class FluidExpand : FluidShader {
    override fun process(fluid: FluidFramebuffer, world: World) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val hardness = world.hardness

        worker.processBalanced(0, w, 8) { x0, x1 ->
            for (x in x0 until x1) {

                /*val sum0 = listOf(
                    srcH, srcVX, srcVY
                ).map { data ->
                    (0 until h).sumOf { y ->
                        data[x + y * w].toDouble()
                    }
                }*/

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

                /*val sum1 = listOf(
                    dstH, dstVX, dstVY
                ).map { data ->
                    (0 until h).sumOf { y ->
                        data[x + y * w].toDouble()
                    }
                }

                for (li in sum0.indices) {
                    if (abs(sum0[li]) !in 0.999 * abs(sum1[li])..1.001 * abs(sum1[li])) {
                        throw IllegalStateException("FluidExpand isn't conservative!, $sum0 -> $sum1 at x=$x")
                    }
                }*/
            }
        }
        fluid.level.swap()
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }
}
