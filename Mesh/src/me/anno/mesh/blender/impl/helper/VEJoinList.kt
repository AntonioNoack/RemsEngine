package me.anno.mesh.blender.impl.helper

import me.anno.mesh.blender.impl.BInstantList
import me.anno.mesh.blender.impl.interfaces.LoopLike
import me.anno.mesh.blender.impl.primitives.BVector1i
import kotlin.math.min

/**
 * helper class to join two lists into one
 * */
class VEJoinList(
    val vs: BInstantList<BVector1i>,
    val es: BInstantList<BVector1i>
) : InstantList<LoopLike>() {
    val tmpInstance = object : LoopLike {
        override val v: Int get() = vs[i].v
        override val e: Int get() = es[i].v
    }
    override val size: Int = min(vs.size, es.size)
    override fun get(index: Int): LoopLike {
        i = index
        return tmpInstance
    }
}