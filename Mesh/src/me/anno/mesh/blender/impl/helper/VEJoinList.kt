package me.anno.mesh.blender.impl.helper

import me.anno.mesh.blender.impl.interfaces.LoopLike
import me.anno.mesh.blender.impl.primitives.BVector1i
import me.anno.utils.structures.lists.SimpleList
import kotlin.math.min

/**
 * helper class to join two lists into one
 * */
class VEJoinList(
    val vs: List<BVector1i>,
    val es: List<BVector1i>
) : SimpleList<LoopLike>() {
    var currentIndex = 0
    val tmpInstance = object : LoopLike {
        override val v: Int get() = vs[currentIndex].v
        override val e: Int get() = es[currentIndex].v
    }
    override val size: Int = min(vs.size, es.size)
    override fun get(index: Int): LoopLike {
        currentIndex = index
        return tmpInstance
    }
}