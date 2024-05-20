package me.anno.mesh.blender.impl.helper

/**
 * helper class to join two lists into one
 * */
abstract class InstantList<V> : List<V> {

    var i = 0

    override fun isEmpty(): Boolean = size <= 0
    override fun iterator(): Iterator<V> = listIterator()
    override fun listIterator(): ListIterator<V> = listIterator(0)
    override fun listIterator(index: Int): ListIterator<V> {
        return object : ListIterator<V> {
            var j = index
            override fun hasNext(): Boolean = j < size
            override fun hasPrevious(): Boolean = j > 0
            override fun next(): V = get(j++)
            override fun nextIndex(): Int = j
            override fun previous(): V = get(--j)
            override fun previousIndex(): Int = j - 1
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        throw NotImplementedError()
    }

    override fun lastIndexOf(element: V): Int {
        throw NotImplementedError()
    }

    override fun indexOf(element: V): Int {
        throw NotImplementedError()
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun contains(element: V): Boolean {
        throw NotImplementedError()
    }
}