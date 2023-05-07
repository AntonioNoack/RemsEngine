package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class MVert(file: BlenderFile, dnaStruct: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, dnaStruct, buffer, position) {

    // they stay the same inside a file
    private val coOffset = getOffset("co[3]")
    val noOffset = getOffset("no[3]") // no longer available with Blender 3.0+ :/

    val x get() = float(coOffset)
    val y get() = float(coOffset + 4)
    val z get() = float(coOffset + 8)

    val nx get() = short(noOffset) / 32767f
    val ny get() = short(noOffset + 4) / 32767f
    val nz get() = short(noOffset + 8) / 32767f

}