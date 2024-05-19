package me.anno.mesh.blender.impl.primitives

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class BVector2i(ptr: ConstructorData) : BlendData(ptr) {

    // they stay the same inside a file
    private val xOffset = getOffset("x")
    private val yOffset = getOffset("y")

    val x get() = int(xOffset)
    val y get() = int(yOffset)

    override fun toString(): String {
        return "($x, $y)"
    }
}