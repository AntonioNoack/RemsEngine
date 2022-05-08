package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.utils.types.Vectors.print
import java.nio.ByteBuffer

class MVert(file: BlenderFile, dnaStruct: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, dnaStruct, buffer, position) {

    // they stay the same inside a file
    private val coOffset = getOffset("co[3]")
    private val noOffset = getOffset("no[3]")

    val x get() = float(coOffset)
    val y get() = float(coOffset + 4)
    val z get() = float(coOffset + 8)

    val nx get() = float(noOffset) / 32767f
    val ny get() = float(noOffset + 4) / 32767f
    val nz get() = float(noOffset + 8) / 32767f

    val pos get() = vec3f(coOffset)
    val normal get() = vec3sNorm(noOffset)

    // then there is a flag, and a weights-indicator

    override fun toString(): String {
        return "[${pos.print()} ${normal.print()}]"
    }

}