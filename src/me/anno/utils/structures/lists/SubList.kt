package me.anno.utils.structures.lists

/**
 * a sublist for UnsafeArrayList
 * */
class SubList<V>(
    val backend: MutableList<V>,
    private val fromIndex: Int,
    private val toIndex: Int
) : MutableList<V> {

    override val size = toIndex - fromIndex

    override fun contains(element: V): Boolean {
        return indexOf(element) >= 0
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun get(index: Int): V {
        return backend[index - fromIndex]
    }

    override fun indexOf(element: V): Int {
        for (i in fromIndex until toIndex) {
            if (backend[i] == element) {
                return i - fromIndex
            }
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<V> = listIterator()

    override fun lastIndexOf(element: V): Int {
        for (i in toIndex - 1 downTo fromIndex) {
            if (backend[i] == element) {
                return i - fromIndex
            }
        }
        return -1
    }

    override fun add(element: V): Boolean {
        throw NotImplementedError()
    }

    override fun add(index: Int, element: V) {
        throw NotImplementedError()
    }

    override fun addAll(index: Int, elements: Collection<V>): Boolean {
        return backend.addAll(index + fromIndex, elements)
    }

    override fun addAll(elements: Collection<V>): Boolean {
        return backend.addAll(toIndex, elements)
    }

    override fun clear() {
        throw NotImplementedError()
    }

    override fun listIterator(): MutableListIterator<V> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<V> {
        return object : MutableListIterator<V> {

            var iterIndex = index
            override fun hasPrevious(): Boolean = iterIndex > 0
            override fun nextIndex(): Int = iterIndex

            override fun previous(): V = backend[--iterIndex]

            override fun previousIndex(): Int = iterIndex - 1

            override fun add(element: V) {
                add(index, element)
            }

            override fun hasNext(): Boolean = iterIndex < size

            override fun next(): V = backend[iterIndex++]

            override fun remove() {
                throw NotImplementedError()
            }

            override fun set(element: V) {
                backend[iterIndex - 1] = element
            }
        }
    }

    override fun remove(element: V): Boolean {
        throw NotImplementedError()
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun removeAt(index: Int): V {
        throw NotImplementedError()
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun set(index: Int, element: V): V {
        val old = backend[index]
        backend[index] = element
        return old
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<V> {
        return SubList(
            backend,
            fromIndex + this.fromIndex,
            toIndex + this.fromIndex
        )
    }
}