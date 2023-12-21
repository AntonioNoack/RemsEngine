package me.anno.tests.game.creeperworld

interface FluidWithNeighborShader : FluidShader {
    override fun process(
        fluid: FluidFramebuffer,
        world: World,
    ) {
        //validateNotOnWall(world.hardness, fluid.level.read)
        //validateNotOnWall(world.hardness, fluid.level.write)
        worker.processBalanced(1, h - 1, 8) { y0, y1 ->
            for (y in y0 until y1) {
                val i0 = y * w
                processInnerPixels(i0 + 1, i0 + w - 1, fluid, world)
            }
        }
        //validateNotOnWall(world.hardness, fluid.level.read)
        //validateNotOnWall(world.hardness, fluid.level.write)
        forAllEdgePixels { x, y, i ->
            processEdgePixel(x, y, i, fluid, world)
        }
        //validateNotOnWall(world.hardness, fluid.level.read)
        //validateNotOnWall(world.hardness, fluid.level.write)
    }

    fun processInnerPixels(
        i0: Int, i1: Int,
        fluid: FluidFramebuffer,
        world: World,
    )

    fun processEdgePixel(
        x: Int, y: Int, i: Int,
        fluid: FluidFramebuffer,
        world: World,
    )
}
