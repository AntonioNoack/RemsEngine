package me.anno.utils.structures.arrays

import kotlin.math.max

@Suppress("unused")
open class ShortArrayList(private val initCapacity: Int) {

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
            array?.copyInto(newArray)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

}