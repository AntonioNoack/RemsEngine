package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueVector
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVVector(ptr: ConstructorData) : BNSValue(ptr) {

    val min = f32("min")
    val max = f32("max")
    val value = f32s("value[4]", 4)

    override fun toString(): String {
        return "Vector { $value, [$min, $max] }"
    }
}