package me.anno.mesh.blender.impl

import me.anno.io.files.Signature
import me.anno.mesh.blender.ConstructorData

class BPackedFile(ptr: ConstructorData) : BLink<BPackedFile>(ptr) {

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