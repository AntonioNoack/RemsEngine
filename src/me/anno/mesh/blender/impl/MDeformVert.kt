package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_meshdata_types.h#L182
 * */
@Suppress("SpellCheckingInspection")
class MDeformVert(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    // { *dw: MDeformWeight, totweight: int, flag: int }
    val weights get() = getInstantList<MDeformWeight>("*dw", numWeights) ?: BInstantList.emptyList()
    val numWeights get() = int("totweight")

    override fun toString(): String {
        return "MDeformVert@$position($weights)"
    }

}