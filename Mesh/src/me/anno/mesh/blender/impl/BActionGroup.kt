package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_action_types.h#L628
 * */
class BActionGroup(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BActionGroup>(file, type, buffer, position) {

    val name = string("name[64]", 64)

    override fun toString(): String {
        return "bActionGroup { '$name' }"
    }

}