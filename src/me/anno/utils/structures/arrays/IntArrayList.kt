package me.anno.utils.structures.arrays

import kotlin.math.min

class IntArrayList(val capacity: Int) {
    private val buffers = ArrayList<IntArray>()
    var size = 0
    operator fun get(index: Int) = buffers[index / capacity][index % capacity]
    operator fun plusAssign(value: Int) {
        val index = size % capacity
        if (index == 0) buffers.add(IntArray(capacity))
        buffers.last()[index] = value
        size++
    }

    fun add(value: Int) {
        plusAssign(value)
    }

    fun toIntArray(): IntArray {
        val dst = IntArray(size)
        for (i in 0 until (size + capacity - 1) / capacity) {
            val src = buffers[i]
            val offset = i * capacity
            System.arraycopy(src, 0, dst, offset, min(capacity, size - offset))
        }
        return dst
    }
}