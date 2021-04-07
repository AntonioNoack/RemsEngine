package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingByteArray(val capacity: Int) {

    var size = 0
        private set

    private var array: ByteArray? = null

    fun add(value: Byte) = plusAssign(value)
    operator fun set(index: Int, value: Byte) {
        array!![index] = value
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Byte) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = ByteArray(if (array == null) capacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}