package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

class RawData(ptr: ConstructorData) : BlendData(ptr) {

    val size get() = block.sizeInBytes
    fun toFloatArray(): FloatArray = f32s(0, (size shr 2).toInt())

    override fun toString(): String = "RawData[$size]"
}