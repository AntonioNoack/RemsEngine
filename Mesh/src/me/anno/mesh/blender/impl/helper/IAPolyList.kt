package me.anno.mesh.blender.impl.helper

import me.anno.mesh.blender.impl.interfaces.PolyLike
import me.anno.mesh.blender.impl.primitives.BVector1i

/**
 * helper class to join two lists into one
 * */
class IAPolyList(
    val loopStartIndices: IntArray,
    val materialIndices: List<BVector1i>,
) : InstantList<PolyLike>() {
    val tmpInstance = object : PolyLike {
        override val loopStart: Int get() = loopStartIndices[i]
        override val loopSize: Int get() = loopStartIndices[i + 1] - loopStart
        override val materialIndex: Int get() = materialIndices.getOrNull(i)?.v ?: 0
    }
    override val size: Int get() = loopStartIndices.size - 1
    override fun get(index: Int): PolyLike {
        i = index
        return tmpInstance
    }
}