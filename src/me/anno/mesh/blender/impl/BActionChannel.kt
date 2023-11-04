package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection")
class BActionChannel(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BActionChannel>(file, type, buffer, position) {

    val name = string("name[64]", 64)

    // *next: bActionChannel, *prev: bActionChannel, *grp: bActionGroup, *ipo: Ipo, constraintChannels: ListBase, flag: int, name[64]: char, temp: int

    override fun toString(): String {
        return "bActionChannel { '$name' }"
    }

}