package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class MLoopCol(ptr: ConstructorData) : BlendData(ptr) {

    private val rOffset = getOffset("r")
    private val gOffset = getOffset("g")
    private val bOffset = getOffset("b")
    private val aOffset = getOffset("a")

    val r get() = byte(rOffset)
    val g get() = byte(gOffset)
    val b get() = byte(bOffset)
    val a get() = byte(aOffset)

}