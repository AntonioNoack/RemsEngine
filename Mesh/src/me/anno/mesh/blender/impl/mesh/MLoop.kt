package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.interfaces.LoopLike

class MLoop(ptr: ConstructorData) : BlendData(ptr), LoopLike {

    private val vOffset = getOffset("v")
    private val eOffset = getOffset("e")

    override val v get() = i32(vOffset)
    override val e get() = i32(eOffset)

    override fun toString(): String {
        return "MLoop { $v, $e }"
    }
}