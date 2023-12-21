package me.anno.tests.game.creeperworld

import me.anno.utils.Color
import me.anno.utils.Color.withAlpha

class FluidTypes(world: CreeperWorld) {

    val creeper = FluidLayer(
        "creeper",
        listOf(
            FluidAccelerate(-0.1f, 0.5f),
            FluidClampVelocity(),
            FluidMove(),
            FluidBlur(1f / 8f, 0.99f, 1f),
            FluidExpand(),
        ),
        1, 0x33ff33.withAlpha(200), world,
    )

    val antiCreeper = FluidLayer(
        "antiCreeper",
        listOf(
            FluidAccelerate(-0.1f, 0.5f),
            FluidClampVelocity(),
            FluidMove(),
            FluidBlur(1f / 8f, 0.99f, 1f),
            FluidExpand(),
        ),
        1, 0x3377ff.withAlpha(140), world,
    )

    val foam = FluidLayer(
        "foam",
        listOf(
            FluidDissolve(creeper.data, antiCreeper.data),
            FluidAccelerate(-0.1f, 0.5f),
            FluidClampVelocity(),
            FluidMove(),
            FluidBlur(1f / 8f, 1f, 0.9f),
            FluidExpand(),
        ),
        1, Color.white.withAlpha(127), world,
    )

    // todo creep + anti-creep = 0
    // todo acid + rock = water
    // todo lava + water = obsidian/rock
    val acid = FluidLayer("acid", listOf(), 2, 0xff8833.withAlpha(100), world)
    val lava = FluidLayer("lava", listOf(), 10, 0xffaa00.withAlpha(255), world)
    val water = FluidLayer("water", listOf(), 1, 0x99aaff.withAlpha(50), world)
    val fluids = listOf(creeper, antiCreeper, foam)
}