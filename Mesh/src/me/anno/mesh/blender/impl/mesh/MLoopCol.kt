package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class MLoopCol(ptr: ConstructorData) : BlendData(ptr) {

    private val rOffset = getOffset("r")
    private val gOffset = getOffset("g")
    private val bOffset = getOffset("b")
    private val aOffset = getOffset("a")

    val r get() = i8(rOffset)
    val g get() = i8(gOffset)
    val b get() = i8(bOffset)
    val a get() = i8(aOffset)

}