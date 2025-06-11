package com.bulletphysics.util

/**
 *
 * @author jezek2
 */
class DoubleArrayList {

    private var array = DoubleArray(16)
    private var size = 0

    fun add(value: Double) {
        if (size == array.size) expand()
        array[size++] = value
    }

    private fun expand() {
        val newArray = DoubleArray(array.size shl 1)
        System.arraycopy(array, 0, newArray, 0, array.size)
        array = newArray
    }

    fun remove(index: Int): Double {
        val old = array[index]
        System.arraycopy(array, index + 1, array, index, size - index - 1)
        size--
        return old
    }

    fun setSize(newSize: Int) {
        if (array.size < newSize) {
            var newSize2 = array.size
            while (newSize2 < newSize) {
                newSize2 = newSize2 shl 1
            }
            val newArray = DoubleArray(newSize2)
            System.arraycopy(array, 0, newArray, 0, array.size)
            array = newArray
        }
        for (i in size until newSize) {
            array[i] = 0.0
        }
        size = newSize
    }


    fun get(index: Int): Double {
        return array[index]
    }

    fun set(index: Int, value: Double) {
        array[index] = value
    }

    fun size(): Int {
        return size
    }
}
