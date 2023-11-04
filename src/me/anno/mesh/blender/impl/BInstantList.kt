package me.anno.mesh.blender.impl

/**
 * saves allocations by using a pseudo-instance, whose position gets adjusted every time an element is accessed;
 * for this to work, all properties inside the child class need to be dynamic getters
 *
 * called "InstantList", because its elements only contain valid data for the instant
 * */
class BInstantList<V : BlendData>(val size: Int, val instance: V?) : Iterable<V> {

    val indices = 0 until size

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

    fun subList(startIndex: Int, endIndex: Int): BInstantList<V> {
        if (startIndex < 0 || endIndex > size || startIndex > endIndex)
            throw IndexOutOfBoundsException()
        if (startIndex == 0 && endIndex == size) return this
        if (instance == null) return this // kinda ok...
        // set position to trick our constructor
        instance.position = position0 + startIndex * typeSize
        return BInstantList(endIndex - startIndex, instance)
    }

    /**
     * Gets a temporary instance to that value at that index.
     * This instance becomes invalid, when get() is called on this list with a different index.
     *
     * Accordingly, this method is not thread-safe.
     * */
    operator fun get(index: Int): V {
        if (index !in 0 until size)
            throw IndexOutOfBoundsException("$index !in 0 until $size")
        val instance = instance!!
        instance.position = position0 + typeSize * index
        return instance
    }

    /**
     * Checks if any instance fulfills the criterion; not thread-safe.
     * */
    inline fun any(lambda: (V) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (lambda(this[i])) {
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return (0 until size).map { get(it).toString() }.toString()
    }

    override fun iterator(): Iterator<V> {
        return object : Iterator<V> {
            private var index = 0
            override fun hasNext(): Boolean = index < size
            override fun next(): V = get(index++)
        }
    }

    companion object {
        fun <V : BlendData> emptyList() = BInstantList<V>(0, null)
    }
}