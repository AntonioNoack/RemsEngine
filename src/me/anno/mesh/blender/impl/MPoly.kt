package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class MPoly(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    private val startOffset = getOffset("loopstart")
    private val sizeOffset = getOffset("totloop")
    private val matOffset = getOffset("mat_nr")

    val loopStart get() = int(startOffset)
    val loopSize get() = int(sizeOffset)
    val materialIndex get() = short(matOffset)

    override fun toString(): String {
        return "MPoly { $loopStart += $loopSize, mat: $materialIndex }"
    }

}