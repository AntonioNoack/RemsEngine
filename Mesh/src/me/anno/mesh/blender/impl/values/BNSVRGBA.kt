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
        Vector4f(f32(valueOffset), f32(valueOffset + 4), f32(valueOffset + 8), f32(valueOffset + 12))
    }

    override fun toString(): String {
        return "RGBA { $value }"
    }
}