package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.interfaces.PolyLike

@Suppress("SpellCheckingInspection", "unused")
class MPoly(ptr: ConstructorData) : BlendData(ptr), PolyLike {

    private val startOffset = getOffset("loopstart")
    private val sizeOffset = getOffset("totloop")
    private val matOffset = getOffset("mat_nr")

    override val loopStart get() = i32(startOffset)
    override val loopSize get() = i32(sizeOffset)
    override val materialIndex get() = u16(matOffset)

    override fun toString(): String {
        return "MPoly { $loopStart += $loopSize, mat: $materialIndex }"
    }
}