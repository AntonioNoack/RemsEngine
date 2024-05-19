package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class MLoop(ptr: ConstructorData) : BlendData(ptr) {

    private val vOffset = getOffset("v")
    private val eOffset = getOffset("e")

    val v get() = int(vOffset)
    val e get() = int(eOffset)

    override fun toString(): String {
        return "MLoop { $v, $e }"
    }

}