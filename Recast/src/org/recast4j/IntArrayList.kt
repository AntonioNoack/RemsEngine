package org.recast4j

import kotlin.math.max

class IntArrayList(var values: IntArray) {

    var size = 0

    constructor(cap: Int = 16) : this(IntArray(cap))
    constructor(src: IntArrayList) : this(src.values.copyOf()) {
        size = src.size
    }

    fun add(v: Int) {
        ensureExtra(1)
        values[size++] = v
    }

    fun add(index: Int, value: Int) {
        add(value)
        System.arraycopy(values, index, values, index + 1, size - 1 - index)
        values[index] = value
    }

    operator fun get(index: Int) = values[index]
    operator fun set(index: Int, value: Int) {
        values[index] = value
    }

    fun remove(index: Int): Int {
        val oldValue = values[index]
        System.arraycopy(values, index + 1, values, index, size - index - 1)
        size--
        return oldValue
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun indexOf(value: Int): Int {
        for (i in 0 until size) {
            if (values[i] == value) return i
        }
        return -1
    }

    fun contains(value: Int): Boolean {
        return indexOf(value) >= 0
    }

    fun clear() {
        size = 0
    }

    fun ensureExtra(extra: Int) {
        ensureCapacity(size + extra)
    }

    private fun ensureCapacity(size: Int) {
        if (size > values.size) {
            values = values.copyOf(max(values.size * 2, max(size, 16)))
        }
    }

    fun isEmpty() = size <= 0

    fun removeRange(startIndex: Int, endIndex: Int) {
        val rangeSize = endIndex - startIndex
        System.arraycopy(values, endIndex, values, startIndex, size - endIndex)
        size -= rangeSize
    }

    fun toIntArray(): IntArray = values.copyOf(size)

    companion object {
        val empty = IntArrayList(0)
    }
}