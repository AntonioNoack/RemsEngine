package com.bulletphysics.util

import kotlin.math.max

/**
 * @author jezek2
 */
class IntArrayList(capacity: Int = 16) {
    private var array = IntArray(max(capacity, 16))
    var size = 0

    fun add(value: Int) {
        if (size == array.size) {
            expand()
        }

        array[size++] = value
    }

    private fun expand() {
        val newArray = IntArray(array.size shl 1)
        System.arraycopy(array, 0, newArray, 0, array.size)
        array = newArray
    }

    fun remove(index: Int): Int {
        val old = array[index]
        System.arraycopy(array, index + 1, array, index, size - index - 1)
        size--
        return old
    }

    fun get(index: Int): Int {
        return array[index]
    }

    fun set(index: Int, value: Int) {
        array[index] = value
    }

    fun size(): Int {
        return size
    }

    fun clear() {
        size = 0
    }
}
