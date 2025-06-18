package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.maths.Maths.min
import me.anno.utils.pooling.Pools

/**
 * container for instanced transforms and their click ids
 * */
open class InstancedStack {

    companion object {
        fun newInstStack(): InstancedStack {
            return InstancedStack()
        }

        fun newAnimStack(): InstancedAnimStack {
            return InstancedAnimStack()
        }

        const val CLEAR_SIZE = 16
    }

    var transforms = Pools.arrayPool[CLEAR_SIZE, false, false]
    var gfxIds = Pools.intArrayPool[CLEAR_SIZE, false, false]
    var size = 0

    open fun clear() {
        transforms = Pools.arrayPool.shrink(transforms, CLEAR_SIZE)
        gfxIds = Pools.intArrayPool.shrink(gfxIds, CLEAR_SIZE)
        size = 0
    }

    fun isNotEmpty() = size > 0
    fun isEmpty() = size == 0

    open fun resize(newSize: Int) {
        transforms = Pools.arrayPool.grow(transforms, newSize)
        gfxIds = Pools.intArrayPool.grow(gfxIds, newSize)
    }

    open fun add(transform: Transform, gfxId: Int) {
        if (size >= min(transforms.size, gfxIds.size)) {
            resize(transforms.size * 2)
        }
        val index = size++
        transforms[index] = transform
        gfxIds[index] = gfxId
    }
}