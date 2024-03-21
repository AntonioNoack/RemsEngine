package me.anno.utils.structures.arrays

import kotlin.math.max

interface NativeArrayList {

    var size: Int
    val capacity: Int

    fun clear() {
        size = 0
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    private fun getNewSize(capacity: Int): Int {
        return max(max(this.capacity * 2, 16), capacity)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity >= this.capacity) {
            resize(getNewSize(capacity))
        }
    }

    fun ensureExtra(extra: Int) {
        ensureCapacity(size + extra)
    }

    fun resize(newSize: Int)
}