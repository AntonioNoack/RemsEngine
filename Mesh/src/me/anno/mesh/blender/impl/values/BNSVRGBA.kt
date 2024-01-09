package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import org.joml.Vector4f
import java.nio.ByteBuffer

/**
 * bNodeSocketValueRGBA
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
class BNSVRGBA(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BNSValue(file, type, buffer, position) {
    val value = run {
        val valueOffset = getOffset("value[4]")
        Vector4f(float(valueOffset), float(valueOffset + 4), float(valueOffset + 8), float(valueOffset + 12))
    }

    override fun toString(): String {
        return "RGBA { $value }"
    }
}