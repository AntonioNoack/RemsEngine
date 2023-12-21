package me.anno.tests.game.creeperworld

class FluidBlur(
    val flow: Float,
    val revFriction: Float,
    val revFluidDecay: Float,
) : FluidWithNeighborShader {

    override fun process(fluid: FluidFramebuffer, world: CreeperWorld) {
        super.process(fluid, world)
        fluid.level.swap()
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: CreeperWorld) {

        val dstH = fluid.level.write
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val hardness = world.hardness

        if (!isSolid(hardness[i])) {
            var sumH = 0f
            var sumVX = 0f
            var sumVY = 0f
            var sumW = 0f

            val srcH = fluid.level.read
            val srcVX = fluid.impulseX.read
            val srcVY = fluid.impulseY.read

            fun addValue(x: Int, y: Int) {
                val j = x + y * world.w
                if (!isSolid(hardness[j])) {
                    sumH += srcH[j] * flow
                    sumVX += srcVX[j] * flow
                    sumVY += srcVY[j] * flow
                    sumW += flow
                }
            }

            if (x > 0) addValue(x - 1, y)
            if (y > 0) addValue(x, y - 1)
            if (x + 1 < world.w) addValue(x + 1, y)
            if (y + 1 < world.h) addValue(x, y + 1)

            val rem = 1f - sumW
            dstH[i] = (srcH[i] * rem + sumH) * revFluidDecay
            dstVX[i] = (srcVX[i] * rem + sumVX) * revFriction
            dstVY[i] = (srcVY[i] * rem + sumVY) * revFriction
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
            if (!isSolid(hardness[i])) {
                var sumH = 0f
                var sumVX = 0f
                var sumVY = 0f
                var sumW = 0f
                fun addValue(dx: Int, dy: Int) {
                    val j = i + dx + dy * w
                    if (!isSolid(hardness[j])) {
                        sumH += srcH[j] * flow
                        sumVX += srcVX[j] * flow
                        sumVY += srcVY[j] * flow
                        sumW += flow
                    }
                }
                addValue(-1, 0)
                addValue(+1, 0)
                addValue(0, -1)
                addValue(0, +1)
                val rem = 1f - sumW
                dstH[i] = (srcH[i] * rem + sumH) * revFluidDecay
                dstVX[i] = (srcVX[i] * rem + sumVX) * revFriction
                dstVY[i] = (srcVY[i] * rem + sumVY) * revFriction
            }
        }
    }
}
