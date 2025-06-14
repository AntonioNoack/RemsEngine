package me.anno.ecs.components.collider

import me.anno.utils.types.Booleans.hasFlag

enum class TwoDims(
    val id: Int, val glslName: String,
    flags: Int, val axis2: Int,
) {
    XY(0, "xy", 3, 2),
    YZ(1, "yz", 6, 0),
    ZX(2, "zx", 5, 1),
    YX(3, "yx", 3, 2),
    ZY(4, "zy", 6, 0),
    XZ(5, "xz", 5, 1);

    val axis0: Int = glslName[0] - 'x'
    val axis1: Int = glslName[1] - 'x'

    val hasX: Boolean = flags.hasFlag(1)
    val hasY: Boolean = flags.hasFlag(2)
    val hasZ: Boolean = flags.hasFlag(4)
}