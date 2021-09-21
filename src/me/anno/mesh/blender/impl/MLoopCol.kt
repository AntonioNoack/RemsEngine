package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class MLoopCol(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    private val rOffset = getOffset("r")
    private val gOffset = getOffset("g")
    private val bOffset = getOffset("b")
    private val aOffset = getOffset("a")

    val r get() = byte(rOffset)
    val g get() = byte(gOffset)
    val b get() = byte(bOffset)
    val a get() = byte(aOffset)

}