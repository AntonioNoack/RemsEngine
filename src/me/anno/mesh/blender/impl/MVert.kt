package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.utils.types.Vectors.print
import java.nio.ByteBuffer

class MVert(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val pos = vec3f(type.byName["co[3]"]!!.offset)
    val normal = vec3sNorm(type.byName["no[3]"]!!.offset)

    // then there is a flag, and a weights-indicator

    override fun toString(): String {
        return "[${pos.print()} ${normal.print()}]"
    }

}