package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueFloat
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVFloat(ptr: ConstructorData) : BNSValue(ptr) {
    val min = float("min")
    val max = float("max")
    val value = float("value")

    override fun toString(): String {
        return "Float { $value, [$min, $max] }"
    }
}