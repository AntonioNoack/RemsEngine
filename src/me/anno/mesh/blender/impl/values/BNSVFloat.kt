package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * bNodeSocketValueFloat
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVFloat(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BNSValue(file, type, buffer, position) {
    val min = float("min")
    val max = float("max")
    val value = float("value")

    override fun toString(): String {
        return "Float { $value, [$min, $max] }"
    }
}