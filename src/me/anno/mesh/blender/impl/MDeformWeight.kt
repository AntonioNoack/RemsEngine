package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_meshdata_types.h#L172
 * */
class MDeformWeight(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    /**
     * Vertex Group Index; unique in arrays
     * */
    val vertexGroupIndex get() = int("def_nr")

    /**
     * Weight between 0.0 and 1.0
     * */
    val weight get() = float("weight")

    override fun toString(): String {
        return "$vertexGroupIndex=$weight"
    }

}