package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class MEdge(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    private val v1Offset = getOffset("v1")
    private val v2Offset = getOffset("v2")

    val v1 get() = int(v1Offset)
    val v2 get() = int(v2Offset)

    // val crease = char("crease")
    // val bweight = char("bweight")
    // val flag = short("flag")

}