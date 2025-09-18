package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.maths.MinMax.min
import me.anno.utils.pooling.ObjectPool
import me.anno.utils.pooling.Pools

/**
 * container for instanced transforms and their click ids
 * */
open class InstancedStack {

    companion object {

        private val instStackPool = ObjectPool(InstancedStack::class.java)
        private val animStackPool = ObjectPool(InstancedAnimStack::class.java)

        fun newInstStack(): InstancedStack {
            return instStackPool.create()
        }

        fun newAnimStack(): InstancedAnimStack {
            return animStackPool.create()
        }

        fun returnStack(value: InstancedStack) {
            when (value::class) {
                InstancedStack::class -> instStackPool.destroy(value)
                InstancedAnimStack::class -> animStackPool.destroy(value as InstancedAnimStack)
            }
        }

        const val CLEAR_SIZE = 64

        private val arrayPool = Pools.arrayPool
        private val intArrayPool = Pools.intArrayPool
    }

    var transforms = arrayPool[CLEAR_SIZE, false, false]
    var gfxIds = intArrayPool[CLEAR_SIZE, false, false]
    var size = 0

    open fun clear() {
        transforms = arrayPool.shrink(transforms, CLEAR_SIZE)
        gfxIds = intArrayPool.shrink(gfxIds, CLEAR_SIZE)
        size = 0
    }

    fun isNotEmpty() = size > 0
    fun isEmpty() = size == 0

    open fun resize(newSize: Int) {
        transforms = arrayPool.grow(transforms, newSize)
        gfxIds = intArrayPool.grow(gfxIds, newSize)
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