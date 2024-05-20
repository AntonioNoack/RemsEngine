package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData
import org.joml.Vector3f

/**
 * bNodeSocketValueVector
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVVector(ptr: ConstructorData) : BNSValue(ptr) {

    val min = f32("min")
    val max = f32("max")
    val value = run {
        val valueOffset = getOffset("value[3]")
        Vector3f(f32(valueOffset), f32(valueOffset + 4), f32(valueOffset + 8))
    }

    override fun toString(): String {
        return "Vector { $value, [$min, $max] }"
    }
}