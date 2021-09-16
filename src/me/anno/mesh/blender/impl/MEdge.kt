package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class MEdge(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {
    val v1 = int("v1")
    val v2 = int("v2")
    // val crease = char("crease")
    // val bweight = char("bweight")
    // val flag = short("flag")
}