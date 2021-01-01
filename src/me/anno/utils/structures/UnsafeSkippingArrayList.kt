package me.anno.utils.structures

import kotlin.math.max
import kotlin.math.min

/**
 * UnsafeArrayList with the ability to remove items
 * */
class UnsafeSkippingArrayList<V> : MutableList<V> {

    var backend = arrayOfNulls<Any?>(1024)
    var removed = BooleanArray(1024)

    // for optimization
    var startIndex = 0

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

    override fun isEmpty(): Boolean = size <= startIndex

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
            val newArray2 = BooleanArray(newArray.size)
            System.arraycopy(removed, 0, newArray2, 0, removed.size)
            removed = newArray2
        }
        backend[size++] = element
        return true
    }

    override fun add(index: Int, element: V) {
        throw NotImplementedError() // cannot move elements to the side
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
        removed = BooleanArray(1024)
    }

    override fun listIterator(): MutableListIterator<V> {
        return listIterator(0)
    }

    private fun findPreviousIndex(index: Int): Int {
        for(i in index-1 downTo 0){
            if(backend[i] != null) return i
        }
        return -1
    }

    private fun findNextIndex(index: Int): Int {
        for(i in index+1 until size){
            if(backend[i] != null) return i
        }
        return size
    }

    override fun listIterator(index: Int): MutableListIterator<V> {
        return object : MutableListIterator<V> {

            private var nextIndex = max(index, startIndex)
            override fun hasPrevious(): Boolean = findPreviousIndex(nextIndex) >= 0
            override fun nextIndex(): Int = nextIndex

            override fun previous(): V  {
                nextIndex = findPreviousIndex(nextIndex)
                return backend[nextIndex] as V
            }

            override fun previousIndex(): Int = nextIndex - 1

            override fun add(element: V) {
                add(index, element)
            }

            override fun hasNext(): Boolean = nextIndex < size

            override fun next(): V {
                val obj = backend[nextIndex] as V
                nextIndex = findNextIndex(nextIndex)
                return obj
            }

            override fun remove() {
                removeAt(findPreviousIndex(nextIndex))
            }

            override fun set(element: V) {
                backend[nextIndex - 1] = element
            }

        }
    }

    override fun remove(element: V): Boolean {
        val index = indexOf(element)
        return if(index < 0){
            false
        } else {
            removeAt(index)
            true
        }
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun removeAt(index: Int): V {
        val obj = backend[index] as V
        removed[index] = true
        backend[index] = null // for GC ;)
        if(index >= startIndex){
            // find the new first index, which has an element
            for(i in startIndex until size){
                if(backend[i] != null){
                    startIndex = i
                    break
                }
            }
        }
        return obj
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        throw NotImplementedError()
    }

    override fun set(index: Int, element: V): V {
        val old = backend[index]
        backend[index] = element
        startIndex = min(startIndex, index)
        return old as V
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<V> {
        throw NotImplementedError()
    }

}