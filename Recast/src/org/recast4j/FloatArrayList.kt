package org.recast4j

import kotlin.math.max

class FloatArrayList(cap: Int = 16) {

    var values = FloatArray(cap)
    var size = 0

    fun add(v: Float) {
        ensureExtra(1)
        values[size++] = v
    }

    fun ensureExtra(extra: Int) {
        ensureCapacity(size + extra)
    }

    private fun ensureCapacity(size: Int) {
        if (values.size < size) {
            values = values.copyOf(max(values.size * 2, max(size, 16)))
        }
    }

    operator fun get(index: Int) = values[index]
}