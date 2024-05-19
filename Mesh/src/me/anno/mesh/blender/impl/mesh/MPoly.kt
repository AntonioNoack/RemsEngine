package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

@Suppress("SpellCheckingInspection", "unused")
class MPoly(ptr: ConstructorData) : BlendData(ptr) {

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