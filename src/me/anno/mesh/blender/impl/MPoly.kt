package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class MPoly(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {
    val loopStart = int("loopstart")
    val loopSize = int("totloop")
    val materialIndex = short("mat_nr")
    // flag?
}