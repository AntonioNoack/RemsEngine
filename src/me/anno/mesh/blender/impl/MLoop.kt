package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class MLoop(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    private val vOffset = getOffset("v")
    private val eOffset = getOffset("e")

    val v get() = int(vOffset)
    val e get() = int(eOffset)
}