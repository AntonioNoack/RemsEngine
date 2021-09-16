package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.utils.types.Vectors.print
import java.nio.ByteBuffer

class MLoopUV(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {
    // val uv = vec2f("uv[2]") save the instantiation
    val u = float(getOffset("uv[2]"))
    val v = float(getOffset("uv[2]") + 4)
    override fun toString(): String = "($u $v)"
}