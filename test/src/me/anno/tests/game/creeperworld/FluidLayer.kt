package me.anno.tests.game.creeperworld

import me.anno.maths.Maths

class FluidLayer(
    val id: String,
    val shaders: List<FluidShader>,
    val viscosity: Int,
    val color: Int
) {
    val data = FluidFramebuffer()
    fun tick(world: World, sumByType: HashMap<String, String>) {
        val fluid = this
        for (shader in fluid.shaders) {
            // val fluidSum = fluid.data.level.read.sum()
            shader.process(fluid.data, world)
            // todo how/where is the fluid multiplying?
            sumByType[fluid.id] = "F: ${fluid.data.level.read.sum()}, E: ${
                (fluid.data.impulseX.read.sumOf { Maths.sq(it.toDouble()) } +
                        fluid.data.impulseY.read.sumOf { Maths.sq(it.toDouble()) }).toFloat()
            }"
            //validateNotOnWall(world.hardness, fluid.data.level.read)
            //validateNotOnWall(world.hardness, fluid.data.level.write)
            /*val fluidSum1 = fluid.data.level.read.sum()
            if (fluidSum1 !in 0.999f * fluidSum..1.001f * fluidSum) {
                throw IllegalStateException("${shader.javaClass} isn't conservative: $fluidSum -> $fluidSum1")
            }*/
        }
    }
}