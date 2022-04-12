package me.anno.ecs.components.mesh.sdf

enum class TwoDims(val id: Int, val glslName: String) {
    XY(0, "xy"), YZ(1, "yz"), ZX(2, "zx"),
    YX(3, "yx"), ZY(4, "zy"), XZ(5, "xz")
}