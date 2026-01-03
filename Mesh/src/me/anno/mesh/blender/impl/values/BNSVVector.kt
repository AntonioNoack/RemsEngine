package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueVector
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVVector(ptr: ConstructorData) : BNSValue(ptr) {

    val min = f32("min")
    val max = f32("max")
    val value =
        if (getField("value[4]") != null) f32s("value[4]", 4) // Blender 5
        else f32s("value[3]", 3) // older Blender

    override fun toString(): String {
        if (value.size == 3) {
            val (x, y, z) = value
            return "Vector { ($x, $y, $z), [$min, $max] }"
        } else {
            val (x, y, z, w) = value
            return "Vector { ($x, $y, $z, $w), [$min, $max] }"
        }
    }
}