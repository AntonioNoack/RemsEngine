package me.anno.utils.structures.arrays

import kotlin.math.max

open class ExpandingGenericArray<V>(private val initCapacity: Int) {

    var size = 0

    var array: Array<Any?>? = null

    fun clear(){
        size = 0
    }

    fun add(value: V) = plusAssign(value)
    operator fun set(index: Int, value: V) {
        array!![index] = value
    }

    operator fun get(index: Int): V = array!![index] as V
    operator fun plusAssign(value: V) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = arrayOfNulls<Any>(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}