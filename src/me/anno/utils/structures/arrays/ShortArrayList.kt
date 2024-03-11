package me.anno.utils.structures.arrays

import kotlin.math.max

@Suppress("unused")
open class ShortArrayList(initCapacity: Int) {

    var size = 0

    fun clear() {
        size = 0
    }

    var array: ShortArray = ShortArray(initCapacity)

    fun add(value: Short) = plusAssign(value)
    operator fun set(index: Int, value: Short) {
        array[index] = value
    }

    fun toArray(): ShortArray = array.copyOf(size)

    operator fun get(index: Int) = array[index]
    operator fun plusAssign(value: Short) {
        if (size + 1 >= array.size) {
            val newSize = max(array.size * 2, 16)
            array = array.copyOf(newSize)
        }
        array[size++] = value
    }
}