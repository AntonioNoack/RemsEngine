package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueFloat
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVFloat(ptr: ConstructorData) : BNSValue(ptr) {
    val min = f32("min")
    val max = f32("max")
    val value = f32("value")

    override fun toString(): String {
        return "Float { $value, [$min, $max] }"
    }
}