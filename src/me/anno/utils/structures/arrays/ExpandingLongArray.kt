package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingLongArray(val capacity: Int) {

    var size = 0
        private set

    private var array: LongArray? = null

    fun add(value: Long) = plusAssign(value)
    operator fun set(index: Int, value: Long) {
        array!![index] = value
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Long) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = LongArray(if (array == null) capacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
        }
        array!![size++] = value
    }

}