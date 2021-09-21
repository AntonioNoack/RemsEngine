package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BLamp(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val r = float("r")
    val g = float("g")
    val b = float("b")

    val type = int("type")

    val energy = float("energy")

    val cascadeExponent = float("cascade_exponent", 4f)
    val cascadeCount = int("cascade_count", 0)

}