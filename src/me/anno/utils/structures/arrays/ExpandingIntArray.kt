package me.anno.utils.structures.arrays

import kotlin.math.max

class ExpandingIntArray(
    private val initCapacity: Int
) {

    var size = 0

    fun clear() {
        size = 0
    }

    private var array: IntArray? = null

    fun add(value: Int) = plusAssign(value)
    operator fun set(index: Int, value: Int) {
        array!![index] = value
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Int) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = IntArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

    fun toIntArray(): IntArray {
        val tmp = IntArray(size)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, size)
        return tmp
    }

    override fun toString(): String {
        val builder = StringBuilder(size * 4)
        builder.append('[')
        if (size > 0) builder.append(this[0])
        for (i in 1 until size) {
            builder.append(',')
            builder.append(this[i])
        }
        builder.append(']')
        return builder.toString()
    }

}