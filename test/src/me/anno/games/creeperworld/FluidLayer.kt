package me.anno.games.creeperworld

class FluidLayer(
    val id: String,
    val density: Float,
    val shaders: List<FluidShader>,
    val viscosity: Int,
    val color: Int,
    world: CreeperWorld
) {
    val data = FluidFramebuffer(world, this)
    fun tick(world: CreeperWorld, sumByType: HashMap<String, String>) {
        val fluid = this
        for (shader in fluid.shaders) {
            // val fluidSum = fluid.data.level.read.sum()
            shader.process(fluid.data, world)
            sumByType[fluid.id] = "${fluid.data.level.read.sum().toInt()}"
            //validateNotOnWall(world.hardness, fluid.data.level.read)
            //validateNotOnWall(world.hardness, fluid.data.level.write)
            /*val fluidSum1 = fluid.data.level.read.sum()
            if (fluidSum1 !in 0.999f * fluidSum..1.001f * fluidSum) {
                throw IllegalStateException("${shader.javaClass} isn't conservative: $fluidSum -> $fluidSum1")
            }*/
        }
    }
}