package me.anno.mesh.blender.impl

import me.anno.utils.structures.lists.SimpleList

/**
 * saves allocations by using a pseudo-instance, whose position gets adjusted every time an element is accessed;
 * for this to work, all properties inside the child class need to be dynamic getters
 *
 * called "InstantList", because its elements only contain valid data for the instant
 * */
class BInstantList<V : BlendData>(override val size: Int, val instance: V?) : SimpleList<V>() {

    private val position0: Int
    private val typeSize: Int

    init {
        if (instance == null) {
            position0 = 0
            typeSize = 0
        } else {
            position0 = instance.position
            typeSize = instance.dnaStruct.type.size
        }
    }

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
        instance.position = position0 + typeSize * index
        return instance
    }

    companion object {
        fun <V : BlendData> emptyList() = BInstantList<V>(0, null)
    }
}