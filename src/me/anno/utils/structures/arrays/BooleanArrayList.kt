package me.anno.utils.structures.arrays

import kotlin.math.max

class BooleanArrayList(val capacity: Int, val defaultValue: Boolean = false) {

    private val buffers = ArrayList<EfficientBooleanArray>()
    var size = 0

    operator fun get(index: Int) = buffers[index / capacity][index % capacity]
    operator fun get(index: Int, defaultValue: Boolean): Boolean {
        val buffer = buffers.getOrNull(index / capacity) ?: return defaultValue
        return buffer[index % capacity]
    }

    operator fun plusAssign(value: Boolean) {
        val index = size % capacity
        if (index == 0) addBuffer()
        buffers.last()[index] = value
        size++
    }

    private fun addBuffer() {
        buffers.add(EfficientBooleanArray(capacity, defaultValue))
    }

    operator fun set(index: Int, value: Boolean) {
        val bufferIndex = index / capacity
        for (i in buffers.size..bufferIndex) {
            addBuffer()
        }
        val localIndex = index % capacity
        buffers[bufferIndex][localIndex] = value
        size = max(index + 1, size)
    }

}