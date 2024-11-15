package me.anno.games.creeperworld

class FluidAccelerate(
    val pressureDiff: Float,
    val gravityY: Float
) : FluidWithNeighborShader {

    override fun process(fluid: FluidFramebuffer, world: CreeperWorld) {
        super.process(fluid, world)
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: CreeperWorld) {

        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write

        val w = world.w
        val srcH0 = srcH[i]
        val density = fluid.layer.density
        val sum = world.weightSum
        val gravity = -gravityY * srcH0//gravityY * (sum[i] - 2f * density * srcH0)
        dstVX[i] = srcVX[i] +
                pressureDiff * (srcH.getOrElse(i + 1) { 0f } - srcH.getOrElse(i - 1) { 0f })
        dstVY[i] = srcVY[i] +
                pressureDiff * (srcH.getOrElse(i + w) { 0f } + srcH.getOrElse(i - w) { 0f }) +
                gravity
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: CreeperWorld) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write

        // gravity depends on the density of fluids at that location
        // todo gravity with densities makes everything unstable

        val density = fluid.layer.density
        val sum = world.weightSum

        val w = world.w
        for (i in i0 until i1) {
            val srcH0 = srcH[i]
            val gravity = -gravityY * srcH0//gravityY * (sum[i] - 2f * density * srcH0)
            dstVX[i] = srcVX[i] + pressureDiff * (srcH[i + 1] - srcH[i - 1])
            dstVY[i] = srcVY[i] + pressureDiff * (srcH[i + w] - srcH[i - w]) + gravity
        }
    }
}
