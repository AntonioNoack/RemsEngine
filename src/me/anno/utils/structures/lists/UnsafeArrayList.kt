package me.anno.utils.structures.lists

/**
 * an arraylist, which does not fail with ConcurrentModificationException,
 * and is only thread-safe when adding from a single Thread
 * (this is the case for our particle system)
 * iterating over it at the same time doesn't matter / needs to be caught separately
 * */
class UnsafeArrayList<V> : MutableList<V> {

    var backend = arrayOfNulls<Any>(1024)

    override var size = 0

    override fun contains(element: V): Boolean {
        throw NotImplementedError()
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun get(index: Int): V {
        return backend[index] as V
    }

    override fun indexOf(element: V): Int {
        throw NotImplementedError()
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<V> {
        return listIterator()
    }

    override fun lastIndexOf(element: V): Int {
        throw NotImplementedError()
    }

    override fun add(element: V): Boolean {
        if (size >= backend.size) {
            val newArray = arrayOfNulls<Any>(backend.size * 2)
            System.arraycopy(backend, 0, newArray, 0, backend.size)
            backend = newArray
        }
        backend[size++] = element
        return true
    }

    override fun add(index: Int, element: V) {
        if (index >= size) add(element)
        else backend[index] = element
    }

    override fun addAll(index: Int, elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun addAll(elements: Collection<V>): Boolean {
        for (element in elements) add(element)
        return true
    }

    override fun clear() {
        size = 0
        backend = arrayOfNulls(1024)
    }

    override fun listIterator(): MutableListIterator<V> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<V> {
        return object : MutableListIterator<V> {

            var index2 = index
            override fun hasPrevious(): Boolean = index2 > 0
            override fun nextIndex(): Int = index2

            override fun previous(): V = backend[--index2] as V

            override fun previousIndex(): Int = index2 - 1

            override fun add(element: V) {
                add(index, element)
            }

            override fun hasNext(): Boolean = index2 < size

            override fun next(): V = backend[index2++] as V

            override fun remove() {
                throw NotImplementedError()
            }

            override fun set(element: V) {
                backend[index2 - 1] = element
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
        return old as V
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<V> {
        return SubList(this, fromIndex, toIndex)
    }

}