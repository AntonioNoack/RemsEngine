package me.anno.utils.structures.lists

abstract class SimpleList<V> : List<V> {

    override fun contains(element: V): Boolean {
        return indexOf(element) >= 0
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        if (elements.isEmpty()) return true
        val remaining = HashSet(elements)
        return indices.any { remaining.remove(this[it]) && remaining.isEmpty() }
    }

    override fun isEmpty(): Boolean = size <= 0
    override fun iterator(): Iterator<V> = listIterator(0)
    override fun listIterator(): ListIterator<V> = listIterator(0)
    override fun listIterator(index: Int): ListIterator<V> = SimpleListIterator(this, index)

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        val self = this
        return object : SimpleList<V>() {
            override val size: Int = toIndex - fromIndex
            override fun get(index: Int): V = self[index + fromIndex]
        }
    }

    override fun lastIndexOf(element: V): Int {
        return indices.indexOfLast { this[it] == element }
    }

    override fun indexOf(element: V): Int {
        return indices.indexOfFirst { this[it] == element }
    }

    override fun toString(): String {
        return joinToString(", ", "[", "]")
    }
}