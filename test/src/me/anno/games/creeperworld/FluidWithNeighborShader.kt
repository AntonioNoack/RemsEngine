package me.anno.games.creeperworld

interface FluidWithNeighborShader : FluidShader {
    override fun process(
        fluid: FluidFramebuffer,
        world: CreeperWorld,
    ) {
        worker.processBalanced(1, world.h - 1, 8) { y0, y1 ->
            for (y in y0 until y1) {
                val i0 = y * world.w
                processInnerPixels(i0 + 1, i0 + world.w - 1, fluid, world)
            }
        }
        forAllEdgePixels(world) { x, y, i ->
            processEdgePixel(x, y, i, fluid, world)
        }
    }

    fun processInnerPixels(
        i0: Int, i1: Int,
        fluid: FluidFramebuffer,
        world: CreeperWorld,
    )

    fun processEdgePixel(
        x: Int, y: Int, i: Int,
        fluid: FluidFramebuffer,
        world: CreeperWorld,
    )
}
