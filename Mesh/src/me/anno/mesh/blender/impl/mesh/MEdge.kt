package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

@Suppress("SpellCheckingInspection", "unused")
class MEdge(ptr: ConstructorData) : BlendData(ptr) {

    private val v1Offset = getOffset("v1")
    private val v2Offset = getOffset("v2")

    val v1 get() = i32(v1Offset)
    val v2 get() = i32(v2Offset)

    // val crease = char("crease")
    // val bweight = char("bweight")
    // val flag = short("flag")

    override fun toString(): String {
        return "MEdge { $v1 - $v2 }"
    }

}