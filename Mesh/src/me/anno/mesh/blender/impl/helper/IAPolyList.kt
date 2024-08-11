package me.anno.mesh.blender.impl.helper

import me.anno.mesh.blender.impl.interfaces.PolyLike
import me.anno.mesh.blender.impl.primitives.BVector1i
import me.anno.utils.structures.lists.SimpleList

/**
 * helper class to join two lists into one
 * */
class IAPolyList(
    val loopStartIndices: IntArray,
    val materialIndices: List<BVector1i>,
) : SimpleList<PolyLike>() {
    var currentIndex = 0
    val tmpInstance = object : PolyLike {
        override val loopStart: Int get() = loopStartIndices[currentIndex]
        override val loopSize: Int get() = loopStartIndices[currentIndex + 1] - loopStart
        override val materialIndex: Int get() = materialIndices.getOrNull(currentIndex)?.v ?: 0
    }
    override val size: Int get() = loopStartIndices.size - 1
    override fun get(index: Int): PolyLike {
        currentIndex = index
        return tmpInstance
    }
}