package me.anno.ecs.components.mesh.sdf

enum class TwoDims(val id: Int, val glslName: String, val flags: Int) {
    XY(0, "xy",3), YZ(1, "yz",6), ZX(2, "zx",5),
    YX(3, "yx",3), ZY(4, "zy",6), XZ(5, "xz",5)
}