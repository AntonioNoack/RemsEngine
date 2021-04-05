package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingIntArrayList(capacity: Int) {

    var size = 0
        private set

    private val array = IntArray(capacity)

    operator fun get(index: Int) = array[index]
    operator fun plusAssign(value: Int) {
        if (size + 1 >= array.size) {
            val newArray = IntArray(max(array.size * 2, 16))
            System.arraycopy(array, 0, newArray, 0, size)
        }
        array[size++] = value
    }

}