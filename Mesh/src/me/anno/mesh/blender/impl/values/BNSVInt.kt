package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueInt
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVInt(ptr: ConstructorData) : BNSValue(ptr) {

    val min = int("min")
    val max = int("max")
    val value = getOffset("value")

    override fun toString(): String {
        return "Int { $value, [$min, $max] }"
    }
}