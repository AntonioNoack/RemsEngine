package me.anno.tests.game.creeperworld

import me.anno.utils.Color
import me.anno.utils.Color.withAlpha

class FluidTypes(world: CreeperWorld) {

    val gravity = -0.1f

    val creeper = FluidLayer(
        "creeper", 0.7f,
        listOf(
            FluidAccelerate(-0.1f, gravity),
            FluidClampVelocity(),
            FluidMove(),
            FluidBlur(1f / 8f, 0.99f, 1f),
            FluidExpand(),
        ),
        1, 0x33ff33.withAlpha(200), world,
    )

    val antiCreeper = FluidLayer(
        "antiCreeper",0.7f,
        listOf(
            FluidAccelerate(-0.1f, gravity),
            FluidClampVelocity(),
            FluidMove(),
            FluidBlur(1f / 8f, 0.99f, 1f),
            FluidExpand(),
        ),
        1, 0x3377ff.withAlpha(140), world,
    )

    val foam = FluidLayer(
        "foam",0.1f,
        listOf(
            FluidDissolveFluid(creeper.data, antiCreeper.data),
            FluidAccelerate(-0.1f, gravity),
            FluidClampVelocity(),
            FluidMove(),
            FluidBlur(1f / 8f, 1f, 0.9f),
            FluidExpand(),
        ),
        1, Color.white.withAlpha(127), world,
    )

    val water = FluidLayer("water", 1f, listOf(
        FluidAccelerate(-0.1f, gravity),
        FluidClampVelocity(),
        FluidMove(),
        FluidBlur(1f / 8f, 0.99f, 1f),
        FluidExpand(),
    ), 1, 0x99aaff.withAlpha(50), world)

    // done creep + anti-creep = 0
    // done acid + stone = water
    // todo lava + water = obsidian/rock
    // todo lava + rock = more lava?
    val acid = FluidLayer("acid", 1.3f, listOf(
        FluidAccelerate(-0.1f, gravity),
        FluidClampVelocity(),
        FluidMove(),
        FluidBlur(1f / 8f, 0.999f, 1f),
        FluidExpand(),
        FluidDissolveRock(5f, 0.1f, water.data),
    ), 1, 0xfcdf5b.withAlpha(100), world)

    val lava = FluidLayer("lava", 3.1f, listOf(), 10, 0xffaa00.withAlpha(255), world)
    val fluids = listOf(creeper, antiCreeper, foam, acid, water)
}