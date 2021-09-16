package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile

class Block(val header: BlockHeader, val data: BinaryFile, val positionInFile: Int) {

    val dataOffset = positionInFile - header.address

    fun offset(o: Int) {
        data.offset(o + dataOffset)
    }

    fun bytes(n: Int): Long {
        return when (n) {
            8 -> data.readLong()
            4 -> data.readInt().toLong()
            2 -> data.readShort().toLong()
            1 -> data.readByte().toLong()
            else -> throw UnsupportedOperationException("$n bytes")
        }
    }

    fun compareTo(o: Long) = header.address - o
    fun contains(address: Long): Boolean {
        return (address - header.address) in 0 until header.size
    }

}