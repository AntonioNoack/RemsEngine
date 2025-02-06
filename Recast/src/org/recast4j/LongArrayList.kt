package org.recast4j

import kotlin.math.max
import kotlin.math.min

class LongArrayList(var values: LongArray) {

    var size = 0

    constructor(cap: Int = 16) : this(LongArray(cap))
    constructor(src: LongArrayList) : this(src.size) {
        System.arraycopy(src.values, 0, values, 0, src.size)
        size = src.size
    }

    fun ensureExtra(extra: Int) {
        ensureCapacity(size + extra)
    }

    private fun ensureCapacity(size: Int) {
        if (values.size < size) {
            values = values.copyOf(max(values.size * 2, max(size, 16)))
        }
    }

    operator fun set(idx: Int, value: Long) {
        values[idx] = value
    }

    fun add(v: Long) {
        ensureExtra(1)
        values[size++] = v
    }

    fun shiftRight(step: Int) {
        if (step > 0) {
            // move everything right
            ensureExtra(step)
            System.arraycopy(values, 0, values, step, size)
        } else {
            // move everything left
            System.arraycopy(values, -step, values, 0, size + step)
        }
        size += step
    }

    operator fun get(index: Int) = values[index]

    fun removeAt(index: Int) {
        System.arraycopy(values, index + 1, values, index, size - index - 1)
        size--
    }

    fun reverse() {
        var j = size - 1
        val values = values
        for (i in 0 until size.shr(1)) {
            val t = values[i]
            values[i] = values[j]
            values[j] = t
            j--
        }
    }

    fun addAll(list: LongArrayList) {
        ensureExtra(list.size)
        System.arraycopy(list.values, 0, values, size, list.size)
        size += list.size
    }

    fun addAll(list: LongArrayList, startIndex: Int, endIndex: Int) {
        val listSize = endIndex - startIndex
        ensureExtra(listSize)
        System.arraycopy(list.values, startIndex, values, size, listSize)
        size += listSize
    }

    fun remove(index: Int): Long {
        val oldValue = values[index]
        System.arraycopy(values, index + 1, values, index, size - index - 1)
        size--
        return oldValue
    }

    fun remove(value: Long) {
        val i = indexOf(value)
        if (i >= 0) remove(i)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun indexOf(value: Long): Int {
        for (i in 0 until size) {
            if (values[i] == value) return i
        }
        return -1
    }

    fun contains(value: Long): Boolean {
        return indexOf(value) >= 0
    }

    fun clear() {
        size = 0
    }

    fun isEmpty() = size <= 0

    fun shrink(newSize: Int) {
        size = min(size, newSize)
    }

    fun subList(startIndex: Int, endIndex: Int): LongArrayList {
        return LongArrayList(values.copyOfRange(startIndex, endIndex))
    }

    companion object {
        val empty = LongArrayList(0)
    }

}