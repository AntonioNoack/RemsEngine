package me.anno.utils.structures.arrays

@Suppress("unused")
open class LongArrayList(initCapacity: Int = 16) : NativeArrayList {

    constructor(list: LongArrayList) : this(list.size) {
        addAll(list)
    }

    override var size = 0

    var values: LongArray = LongArray(initCapacity)
    override val capacity: Int get() = values.size

    fun addAll(list: LongArrayList, i0: Int = 0, len: Int = list.size - i0) {
        for (i in i0 until i0 + len) {
            add(list[i])
        }
    }

    fun add(value: Long) = plusAssign(value)
    operator fun set(index: Int, value: Long) {
        values[index] = value
    }

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    operator fun get(index: Int) = values[index]
    operator fun plusAssign(value: Long) {
        ensureExtra(1)
        values[size++] = value
    }

    fun reverse() {
        values.reverse(0, size)
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

    fun removeAt(index: Int): Long {
        val value = values[index]
        System.arraycopy(values, index + 1, values, index, size - index - 1)
        size--
        return value
    }


    fun remove(value: Long) {
        val i = indexOf(value)
        if (i >= 0) removeAt(i)
    }

    fun subList(s: Int, e: Int): LongArrayList {
        val list = LongArrayList(e - s)
        val values = values
        for (i in s until e) list.add(values[i])
        return list
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
}