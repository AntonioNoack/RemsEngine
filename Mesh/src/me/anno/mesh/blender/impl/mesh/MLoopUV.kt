package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData
import me.anno.mesh.blender.impl.interfaces.UVLike

class MLoopUV(ptr: ConstructorData) : BlendData(ptr), UVLike {

    // val uv = vec2f("uv[2]") save the instantiation
    // save the lookup
    private val uvOffset = getOffset("uv[2]")

    override val u get() = f32(uvOffset)
    override val v get() = f32(uvOffset + 4)

    override fun toString(): String = "($u $v)"

}