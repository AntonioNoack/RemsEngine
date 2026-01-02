package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.files.Files.formatFileSize

class Block(file: BinaryFile) {

    val code = file.readBlockCode()
    val sizeInBytes: Long
    val address: Long // old address of the block -> todo what are the new pointers???
    val sdnaIndex: Int
    val count: Long

    init {
        if (file.isLegacyFile) {
            sizeInBytes = file.readInt().toLong() and 0xffff_ffffL // len
            address = file.readPointer() // old
            sdnaIndex = file.readInt() // SDNAnr
            count = file.readInt().toLong() and 0xffff_ffffL
        } else {
            sdnaIndex = file.readInt()
            address = file.readPointer()
            sizeInBytes = file.readPointer()
            count = file.readPointer()
        }
    }

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
        return (address - this.address) in 0 until sizeInBytes
    }

    override fun toString(): String {
        return "Block[${code.r().toChar()}${code.g().toChar()}${code.b().toChar()}${code.a().toChar()}]" +
                "[${sizeInBytes.formatFileSize()}, @$address, type $sdnaIndex, ${count}x]@${positionInFile.toString(16)}"
    }

}