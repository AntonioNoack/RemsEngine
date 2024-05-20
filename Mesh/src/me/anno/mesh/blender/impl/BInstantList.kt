package me.anno.mesh.blender.impl

/**
 * saves allocations by using a pseudo-instance, whose position gets adjusted every time an element is accessed;
 * for this to work, all properties inside the child class need to be dynamic getters
 *
 * called "InstantList", because its elements only contain valid data for the instant
 * */
class BInstantList<V : BlendData>(override val size: Int, val instance: V?) : List<V> {

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

    override fun iterator(): Iterator<V> = listIterator()
    override fun listIterator(): ListIterator<V> = listIterator(0)
    override fun listIterator(index: Int): ListIterator<V> {
        return object : ListIterator<V> {
            private var i = index
            override fun hasNext(): Boolean = i < size
            override fun next(): V = get(i++)
            override fun hasPrevious(): Boolean = i > 0
            override fun nextIndex(): Int = i
            override fun previousIndex(): Int = i - 1
            override fun previous(): V = get(--i)
        }
    }

    override fun isEmpty(): Boolean = size <= 0

    override fun indexOf(element: V): Int {
        throw NotImplementedError()
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        throw NotImplementedError()
    }

    override fun lastIndexOf(element: V): Int {
        throw NotImplementedError()
    }

    override fun contains(element: V): Boolean = indexOf(element) >= 0
    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all { contains(it) }
    }

    companion object {
        fun <V : BlendData> emptyList() = BInstantList<V>(0, null)
    }
}