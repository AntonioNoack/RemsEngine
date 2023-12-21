package me.anno.tests.game.creeperworld

fun interface FluidShader {
    fun process(
        fluid: FluidFramebuffer,
        world: World,
    )
}