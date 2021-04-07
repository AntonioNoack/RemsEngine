package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingLongArray(
    private val initCapacity: Int
) {

    var size = 0

    private var array: LongArray? = null

    fun add(value: Long) = plusAssign(value)
    operator fun set(index: Int, value: Long) {
        array!![index] = value
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Long) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = LongArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}