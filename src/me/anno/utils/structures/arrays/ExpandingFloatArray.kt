package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingFloatArray(
    private val initCapacity: Int
) {

    var size = 0

    private var array: FloatArray? = null

    fun add(value: Float) = plusAssign(value)
    operator fun set(index: Int, value: Float) {
        array!![index] = value
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Float) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = FloatArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}