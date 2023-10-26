package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import org.joml.Vector3f
import java.nio.ByteBuffer

/**
 * bNodeSocketValueRotation
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVRotation(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BNSValue(file, type, buffer, position) {

    val value = run {
        val valueOffset = getOffset("value_euler[3]") // order???
        Vector3f(float(valueOffset), float(valueOffset + 4), float(valueOffset + 8))
    }

    override fun toString(): String {
        return "Rotation { $value }"
    }

}