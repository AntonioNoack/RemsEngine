package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_meshdata_types.h#L172
 * */
class MDeformWeight(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val defNrOffset = getOffset("def_nr")
    val weightOffset = getOffset("weight")

    /**
     * Vertex Group Index; unique in arrays
     * */
    val vertexGroupIndex get() = int(defNrOffset)

    /**
     * Weight between 0.0 and 1.0
     * */
    val weight get() = float(weightOffset)

    override fun toString(): String {
        return "[$vertexGroupIndex=$weight @$position]"
    }
}