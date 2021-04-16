package me.anno.utils.structures.lists

/**
 * an arraylist, which does not fail with ConcurrentModificationException,
 * and is only thread-safe when modifying from a single Thread
 * (this is the case for our particle system)
 * iterating over it at the same time doesn't matter / needs to be caught separately
 * */
class UnsafeArrayList<V>(capacity0: Int = 16) : MutableList<V> {

    var backend = arrayOfNulls<Any>(capacity0)

    override var size = 0

    override fun contains(element: V): Boolean {
        for (i in 0 until size) {
            if (element == backend[i]) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        for (element in elements) {
            if (!contains(element)) return false
        }
        return true
    }

    override fun get(index: Int): V {
        return backend[index] as V
    }

    override fun indexOf(element: V): Int {
        for (i in 0 until size) {
            if (element == backend[i]) return i
        }
        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<V> {
        return listIterator()
    }

    override fun lastIndexOf(element: V): Int {
        for (i in size - 1 downTo 0) {
            if (element == backend[i]) return i
        }
        return -1
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
        val index = indexOf(element)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        var writeIndex = 0
        for (readIndex in 0 until size) {
            val element = backend[readIndex]
            if (element !in elements) {
                backend[writeIndex++] = element
            }// else writeIndex not increasing
        }
        val hasChanged = size != writeIndex
        size = writeIndex
        return hasChanged
    }

    override fun removeAt(index: Int): V {
        val element = backend[index]
        size--
        for (i in index until size) {
            backend[i] = backend[i + 1]
        }
        return element as V
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        var writeIndex = 0
        for (readIndex in 0 until size) {
            val element = backend[readIndex]
            if (element in elements) {
                backend[writeIndex++] = element
            }// else writeIndex not increasing
        }
        val hasChanged = size != writeIndex
        size = writeIndex
        return hasChanged
    }

    override fun set(index: Int, element: V): V {
        val old = backend[index] as V
        backend[index] = element
        return old
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<V> {
        return SubList(this, fromIndex, toIndex)
    }

}