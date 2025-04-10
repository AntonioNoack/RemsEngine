package me.anno.utils.structures.lists

abstract class SimpleList<V> : List<V> {

    class SimpleSubList<V>(
        val fromIndex: Int, toIndex: Int,
        val base: SimpleList<V>
    ) : SimpleList<V>() {
        override val size: Int = toIndex - fromIndex
        override fun get(index: Int): V = base[index + fromIndex]
    }

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
        return if (fromIndex == 0 && toIndex == size) this
        else if (this is SimpleSubList<V>) {
            val offset = this.fromIndex
            SimpleSubList(fromIndex + offset, toIndex + offset, base)
        } else {
            SimpleSubList(fromIndex, toIndex, this)
        }
    }

    override fun lastIndexOf(element: V): Int {
        for (i in size - 1 downTo 0) {
            if (this[i] == element) return i
        }
        return -1
    }

    override fun indexOf(element: V): Int {
        for (i in 0 until size) {
            if (this[i] == element) return i
        }
        return -1
    }

    override fun toString(): String {
        return joinToString(", ", "[", "]")
    }
}