package me.anno.utils.structures.arrays

import kotlin.math.max

@Suppress("unused")
open class LongArrayList(initCapacity: Int) {

    var size = 0

    fun clear() {
        size = 0
    }

    var array: LongArray = LongArray(initCapacity)

    fun add(value: Long) = plusAssign(value)
    operator fun set(index: Int, value: Long) {
        array[index] = value
    }

    fun toArray(): LongArray = array.copyOf(size)

    operator fun get(index: Int) = array[index]
    operator fun plusAssign(value: Long) {
        if (size + 1 >= array.size) {
            val newSize = max(array.size * 2, 16)
            array = array.copyOf(newSize)
        }
        array[size++] = value
    }
}