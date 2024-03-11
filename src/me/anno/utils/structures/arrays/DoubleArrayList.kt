package me.anno.utils.structures.arrays

import kotlin.math.max

@Suppress("unused")
open class DoubleArrayList(initCapacity: Int) {

    var size = 0

    fun clear() {
        size = 0
    }

    var array: DoubleArray = DoubleArray(initCapacity)

    fun add(value: Double) = plusAssign(value)
    operator fun set(index: Int, value: Double) {
        array[index] = value
    }

    fun toArray(): DoubleArray = array.copyOf(size)

    operator fun get(index: Int) = array[index]
    operator fun plusAssign(value: Double) {
        if (size + 1 >= array.size) {
            val newSize = max(array.size * 2, 16)
            array = array.copyOf(newSize)
        }
        array[size++] = value
    }
}