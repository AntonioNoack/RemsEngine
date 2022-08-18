package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingDoubleArray(private val initCapacity: Int) {

    var size = 0

    fun clear() {
        size = 0
    }

    var array: DoubleArray? = null

    fun add(value: Double) = plusAssign(value)
    operator fun set(index: Int, value: Double) {
        array!![index] = value
    }

    fun toArray(): DoubleArray {
        if (array == null) return DoubleArray(0)
        val array = array!!
        return DoubleArray(size) { array[it] }
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Double) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = DoubleArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}