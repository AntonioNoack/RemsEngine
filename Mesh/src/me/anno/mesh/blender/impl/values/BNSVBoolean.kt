package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueBoolean
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVBoolean(ptr: ConstructorData) : BNSValue(ptr) {

    val value = i8("value") != 0.toByte()

    override fun toString(): String {
        return value.toString()
    }
}