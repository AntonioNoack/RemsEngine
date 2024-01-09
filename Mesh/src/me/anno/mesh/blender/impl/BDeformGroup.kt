package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BDeformGroup(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BDeformGroup>(file, type, buffer, position) {

    // { *next: bDeformGroup, *prev: bDeformGroup, name[64]: char, flag: char, _pad0[7]: char }

    val name = string("name[64]", 64)

    override fun toString(): String {
        return "bDeformGroup { '$name' }"
    }

}