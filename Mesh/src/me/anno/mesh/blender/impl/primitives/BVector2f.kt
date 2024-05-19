package me.anno.mesh.blender.impl.primitives

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.interfaces.UVLike

class BVector2f(ptr: ConstructorData) : BlendData(ptr), UVLike {

    // they stay the same inside a file
    private val xOffset = getOffset("x")
    private val yOffset = getOffset("y")

    val x get() = float(xOffset)
    val y get() = float(yOffset)

    override val u: Float get() = x
    override val v: Float get() = y

    override fun toString(): String {
        return "($x, $y)"
    }
}