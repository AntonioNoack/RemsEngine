package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile

class BlockHeader(file: BinaryFile) {

    init {
        read(file)
    }

    var code = 0
    var size = 0
    var address = 0L
    var sdnaIndex = 0
    var count = 0

    fun read(file: BinaryFile) {
        code = file.readRGBA()
        size = file.readInt()
        address = file.readLong()
        sdnaIndex = file.readInt()
        count = file.readInt()
    }

}