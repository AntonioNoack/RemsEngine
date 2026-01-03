package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData

/**
 * bNodeSocketValueRGBA
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVRGBA(ptr: ConstructorData) : BNSValue(ptr) {

    val value = f32s("value[4]", 4)

    override fun toString(): String {
        return "RGBA { $value }"
    }
}