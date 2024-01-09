package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct

class Block(file: BinaryFile) {

    val code = file.readRGBA()
    val size = file.readInt()
    val address = file.readLong()
    val sdnaIndex = file.readInt()
    val count = file.readInt()
    val positionInFile = file.index // must come last

    /**
     * add this to convert an address into a position within the file;
     * subtract this to convert a position within the file into an address
     * */
    val dataOffset = positionInFile - address

    fun getType(file: BlenderFile): DNAStruct {
        return file.structs[sdnaIndex]
    }

    fun getTypeName(file: BlenderFile): String {
        return file.structs[sdnaIndex].type.name
    }

    fun compareTo(o: Long) = address - o
    fun contains(address: Long): Boolean {
        return (address - this.address) in 0 until size
    }

    override fun toString(): String {
        return "Block@${positionInFile.toString(16)}"
    }

}