package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.ConstructorData
import org.joml.Vector3f

/**
 * bNodeSocketValueRotation
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVRotation(ptr: ConstructorData) : BNSValue(ptr) {

    val value = run {
        val valueOffset = getOffset("value_euler[3]") // order???
        Vector3f(float(valueOffset), float(valueOffset + 4), float(valueOffset + 8))
    }

    override fun toString(): String {
        return "Rotation { $value }"
    }
}