package me.anno.mesh.blender.impl

import me.anno.utils.structures.lists.SimpleList

/**
 * List except that only one element at a time is valid (the last accessed element).
 * This saves tons of allocations.
 *
 * All properties in the child class must be getters for this to work.
 * Offsets can be cached for better performance, because they cannot change within a list.
 * */
class BInstantList<V : BlendData>(override val size: Int, val stride: Int, val instance: V?) : SimpleList<V>() {

    val positionInFile: Int = instance?.positionInFile ?: 0

    /**
     * Gets a temporary instance to that value at that index.
     * This instance becomes invalid, when get() is called on this list with a different index.
     *
     * Accordingly, this method is not thread-safe.
     * */
    override operator fun get(index: Int): V {
        if (index !in 0 until size) {
            throw IndexOutOfBoundsException("$index !in 0 until $size")
        }
        val instance = instance!!
        instance.positionInFile = positionInFile + stride * index
        return instance
    }
}