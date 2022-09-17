package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile

class BlockHeader(file: BinaryFile) {
    val code = file.readRGBA()
    val size = file.readInt()
    val address = file.readLong()
    val sdnaIndex = file.readInt()
    val count = file.readInt()
}