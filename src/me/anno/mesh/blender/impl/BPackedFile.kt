package me.anno.mesh.blender.impl

import me.anno.io.files.Signature
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BPackedFile(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BPackedFile>(file, type, buffer, position) {

    val size = int("size")
    val seek = int("seek") // offset??
    val data by lazy {
        // data could become complicated, if it was split into multiple blocks
        val pointer = pointer(getOffset("*data"))
        val block = file.blockTable.findBlock(file, pointer)!!
        val dataPosition = pointer + block.dataOffset
        raw(dataPosition.toInt(), size)
    }

    override fun toString(): String {
        return "PackedFile { $size, $seek, ${Signature.find(data)} }"
    }
}