package me.anno.utils.structures.lists

import me.anno.utils.assertions.assertTrue

/**
 * a sublist for UnsafeArrayList
 * */
class SubList<V>(
    val backend: MutableList<V>,
    private val fromIndex: Int,
    private val toIndex: Int
) : MutableList<V>, SimpleList<V>() {

    override val size = toIndex - fromIndex

    override fun get(index: Int): V {
        return backend[index - fromIndex]
    }

    override fun iterator(): MutableIterator<V> = listIterator()

    override fun add(element: V): Boolean {
        add(size, element)
        return true
    }

    override fun add(index: Int, element: V) {
        assertTrue(index in 0..size)
        backend.add(index + fromIndex, element)
    }

    override fun addAll(index: Int, elements: Collection<V>): Boolean {
        return backend.addAll(index + fromIndex, elements)
    }

    override fun addAll(elements: Collection<V>): Boolean {
        return backend.addAll(toIndex, elements)
    }

    override fun clear() {
        // unoptimized...
        for (i1 in toIndex - 1 downTo fromIndex) {
            backend.removeAt(i1)
        }
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
                removeAt(iterIndex)
                iterIndex-- // ok like that?
            }

            override fun set(element: V) {
                backend[iterIndex - 1] = element
            }
        }
    }

    override fun remove(element: V): Boolean {
        val index = indexOf(element)
        if (index >= 0) removeAt(index)
        return index >= 0
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        return removeIf { e -> e in elements }
    }

    override fun removeAt(index: Int): V {
        assertTrue(index in indices)
        return backend.removeAt(index + fromIndex)
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        return removeIf { e -> e !in elements }
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