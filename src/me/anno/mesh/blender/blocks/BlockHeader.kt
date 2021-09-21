package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r

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

    override fun toString(): String {
        return "code: ${code.r().toChar()}${code.g().toChar()}${code.b().toChar()}${code.a().toChar()}," +
                " size: $size, address: $address, sdnaIndex: $sdnaIndex, count: $count"
    }

}