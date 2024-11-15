package me.anno.games.creeperworld

fun interface FluidShader {
    fun process(
        fluid: FluidFramebuffer,
        world: CreeperWorld,
    )
}