package me.anno.mesh.blender.impl.primitives

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class MIntProperty(ptr: ConstructorData) : BlendData(ptr) {

    // they stay the same inside a file
    private val valueOffset = getOffset("i")

    val v get() = int(valueOffset)

    override fun toString(): String {
        return v.toString()
    }
}