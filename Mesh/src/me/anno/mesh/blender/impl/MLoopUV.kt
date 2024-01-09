package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class MLoopUV(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    // val uv = vec2f("uv[2]") save the instantiation
    // save the lookup
    private val uvOffset = getOffset("uv[2]")

    val u get() = float(uvOffset)
    val v get() = float(uvOffset + 4)

    override fun toString(): String = "($u $v)"

}