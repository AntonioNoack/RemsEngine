package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingShortArray(private val initCapacity: Int) {

    var size = 0

    fun clear(){
        size = 0
    }

    var array: ShortArray? = null

    fun add(value: Short) = plusAssign(value)
    operator fun set(index: Int, value: Short) {
        array!![index] = value
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Short) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = ShortArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}