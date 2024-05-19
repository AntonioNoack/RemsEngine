package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData
import org.joml.Vector4f

/**
 * bNodeSocketValueRGBA
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVRGBA(ptr: ConstructorData) : BNSValue(ptr) {
    val value = run {
        val valueOffset = getOffset("value[4]")
        Vector4f(float(valueOffset), float(valueOffset + 4), float(valueOffset + 8), float(valueOffset + 12))
    }

    override fun toString(): String {
        return "RGBA { $value }"
    }
}