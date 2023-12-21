package me.anno.tests.game.creeperworld

class FluidClampVelocity : FluidWithNeighborShader {

    fun safeClamp(v: Float, min: Float, max: Float): Float {
        return if (v < min) min
        else if (v > max) max
        else if (v < max) v
        else 0f // NaN
    }

    override fun processEdgePixel(x: Int, y: Int, i: Int, fluid: FluidFramebuffer, world: CreeperWorld) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val h = srcH[i]
        dstVX[i] = safeClamp(
            srcVX[i],
            check1(x - 1, y, 0, 1, -h, world),
            check1(x + 1, y, 0, 1, h, world)
        )
        dstVY[i] = safeClamp(
            srcVY[i],
            check1(x, y - 1, 1, 0, -h, world),
            check1(x, y + 1, 1, 0, h, world)
        )
    }

    fun check1(x: Int, y: Int, dx: Int, dy: Int, v: Float, world: CreeperWorld): Float {
        return if (check1(x, y, world)
            && check1(x + dx, y + dy, world)
            && check1(x - dx, y - dy, world)
        ) v
        else 0f
    }

    fun check1(x: Int, y: Int, world: CreeperWorld): Boolean {
        return x in 0 until world.w && y in 0 until world.h && !isSolid(world.hardness[x + y * world.w])
    }

    fun check(i: Int, di: Int, v: Float, hardness: FloatArray): Float {
        return if (
            isSolid(hardness[i]) ||
            isSolid(hardness[i - di]) ||
            isSolid(hardness[i + di])
        ) 0f else v
    }

    fun check(i: Int, v: Float, hardness: FloatArray): Float {
        return if (isSolid(hardness[i])) 0f else v
    }

    override fun processInnerPixels(i0: Int, i1: Int, fluid: FluidFramebuffer, world: CreeperWorld) {
        val srcH = fluid.level.read
        val srcVX = fluid.impulseX.read
        val srcVY = fluid.impulseY.read
        val dstVX = fluid.impulseX.write
        val dstVY = fluid.impulseY.write
        val w = world.w
        val hardness = world.hardness
        for (i in i0 until i1) {
            val h = srcH[i]
            dstVX[i] = safeClamp(
                srcVX[i],
                check(i - 1, w, -h, hardness),
                check(i + 1, w, +h, hardness)
            )
            dstVY[i] = safeClamp(
                srcVY[i],
                check(i - w, 1, -h, hardness),
                check(i + w, 1, +h, hardness)
            )
        }
    }

    override fun process(fluid: FluidFramebuffer, world: CreeperWorld) {
        super.process(fluid, world)
        fluid.impulseX.swap()
        fluid.impulseY.swap()
    }
}
