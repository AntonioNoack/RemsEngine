package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct

class Block(val header: BlockHeader, val positionInFile: Int) {

    val dataOffset = positionInFile - header.address

    fun getType(file: BlenderFile): DNAStruct {
        return file.structs[header.sdnaIndex]
    }

    fun getTypeName(file: BlenderFile): String {
        return file.structs[header.sdnaIndex].type.name
    }

    fun compareTo(o: Long) = header.address - o
    fun contains(address: Long): Boolean {
        return (address - header.address) in 0 until header.size
    }

    override fun toString(): String {
        return "$header, in file: $positionInFile"
    }

}