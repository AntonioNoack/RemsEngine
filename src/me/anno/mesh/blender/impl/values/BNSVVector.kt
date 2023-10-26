package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import org.joml.Vector3f
import java.nio.ByteBuffer

/**
 * bNodeSocketValueVector
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVVector(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BNSValue(file, type, buffer, position) {

    val min = float("min")
    val max = float("max")
    val value = run {
        val valueOffset = getOffset("value[3]")
        Vector3f(float(valueOffset), float(valueOffset + 4), float(valueOffset + 8))
    }

    override fun toString(): String {
        return "Vector { $value, [$min, $max] }"
    }

}