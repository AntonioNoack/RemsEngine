package me.anno.mesh.blender.blocks

import me.anno.mesh.blender.BinaryFile
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r

class BlockHeader(file: BinaryFile) {

    val code = file.readRGBA()
    val size = file.readInt()
    val address = file.readLong()
    val sdnaIndex = file.readInt()
    val count = file.readInt()

    override fun toString(): String {
        return "code: ${code.r().toChar()}${code.g().toChar()}${code.b().toChar()}${code.a().toChar()}," +
                " size: $size, address: $address, sdnaIndex: $sdnaIndex, count: $count"
    }

}