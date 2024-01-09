package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * bNodeSocketValueInt
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVInt(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BNSValue(file, type, buffer, position) {

    val min = int("min")
    val max = int("max")
    val value = getOffset("value")

    override fun toString(): String {
        return "Int { $value, [$min, $max] }"
    }
}