package me.anno.mesh.blender.impl.primitives

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class BVector3f(ptr: ConstructorData) : BlendData(ptr) {

    // they stay the same inside a file
    private val xOffset = getOffset("x")
    private val yOffset = getOffset("y")
    private val zOffset = getOffset("z")

    val x get() = float(xOffset)
    val y get() = float(yOffset)
    val z get() = float(zOffset)

    override fun toString(): String {
        return "($x, $y, $z)"
    }
}