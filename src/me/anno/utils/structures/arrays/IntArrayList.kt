package me.anno.utils.structures.arrays

import me.anno.cache.ICacheData
import me.anno.utils.pooling.IntArrayPool
import me.anno.utils.search.BinarySearch
import me.anno.utils.types.Strings.joinChars0

open class IntArrayList(initCapacity: Int, val pool: IntArrayPool? = null) : NativeArrayList, ICacheData {

    companion object {
        val emptyIntList = IntArrayList(0)
    }

    override var size = 0
        set(value) {
            field = value
            ensureCapacity(value)
        }

    var values = alloc(initCapacity)
    override val capacity: Int get() = values.size

    fun alloc(size: Int): IntArray {
        return if (pool != null) pool[size, true, false] else IntArray(size)
    }

    fun removeLast(): Int {
        return values[--size]
    }

    fun add(value: Int) = plusAssign(value)

    override fun resize(newSize: Int) {
        values = values.copyOf(newSize)
    }

    fun inc(position: Int) {
        values[position]++
    }

    fun dec(position: Int) {
        values[position]--
    }

    fun add(index: Int, value: Int) {
        ensureCapacity(size + 1)
        val values = values
        values.copyInto(values, index + 1, index, size)
        values[index] = value
        size++
    }

    fun add(values: List<Int>, index0: Int = 0, index1: Int = values.size) {
        var size = size
        val length = index1 - index0
        ensureCapacity(size + length)
        val array = this.values
        for (index in index0 until index1) {
            array[size++] = values[index]
        }
        this.size = size
    }

    fun addUnsafe(src: IntArray, startIndex: Int = 0, length: Int = src.size - startIndex) {
        src.copyInto(values, size, startIndex, startIndex + length)
        size += length
    }

    @Suppress("unused")
    fun addUnsafe(src: IntArrayList, startIndex: Int, length: Int) {
        addUnsafe(src.values, startIndex, length)
    }

    fun add(v: IntArray, srcStartIndex: Int = 0, length: Int = v.size - srcStartIndex) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun add(values: IntArrayList, srcStartIndex: Int = 0, length: Int = values.size - srcStartIndex) {
        add(values.values, srcStartIndex, length)
    }

    fun joinChars(startIndex: Int = 0, endIndex: Int = size): CharSequence {
        val builder = StringBuilder(endIndex - startIndex)
        // could be optimized
        for (index in startIndex until endIndex) {
            val char = get(index)
            builder.append(char.joinChars0())
        }
        return builder
    }

    fun removeAt(index: Int): Int {
        val array = values
        val value = array[index]
        removeBetween(index, index + 1)
        return value
    }

    fun removeBetween(index0: Int, index1: Int) {
        val length = index1 - index0
        values.copyInto(values, index0, index1, size)
        size -= length
    }

    operator fun set(index: Int, value: Int) {
        values[index] = value
    }

    fun last() = values[size - 1]

    operator fun get(index: Int) = values[index]
    fun getOrNull(index: Int) = values.getOrNull(index)

    @Suppress("unused")
    fun addUnsafe(x: Int) {
        values[size++] = x
    }

    @Suppress("unused")
    fun addUnsafe(x: Int, y: Int) {
        val array = values
        var size = size
        array[size++] = x
        array[size++] = y
        this.size = size
    }

    fun add(x: Int, y: Int, z: Int) {
        ensureCapacity(size + 3)
        val array = values
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    @Suppress("unused")
    fun addUnsafe(x: Int, y: Int, z: Int) {
        val array = values
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    operator fun plusAssign(value: Int) {
        ensureCapacity(size + 1)
        values[size++] = value
    }

    fun isEmpty(): Boolean = size <= 0

    fun contains(element: Int): Boolean {
        return indexOf(element) >= 0
    }

    fun indexOf(element: Int): Int {
        val array = values
        for (i in 0 until size) {
            if (array[i] == element) return i
        }
        return -1
    }

    fun lastIndexOf(element: Int): Int {
        val array = values
        for (i in size - 1 downTo 0) {
            if (array[i] == element) return i
        }
        return -1
    }

    fun subList(fromIndex: Int, toIndex: Int): List<Int> {
        val array = values
        return IntArray(toIndex - fromIndex) { array[fromIndex + it] }.toList()
    }

    @Suppress("unused")
    fun binarySearch(element: Int): Int {
        return BinarySearch.binarySearch(size) { index ->
            this[index].compareTo(element)
        }
    }

    fun toIntArray(canReturnSelf: Boolean = true, exact: Boolean = true) = toIntArray(size, canReturnSelf, exact)
    fun toList(): List<Int> = subList(0, size)
    val indices: IntRange get() = 0 until size
    val lastIndex: Int get() = size - 1
    fun isNotEmpty(): Boolean = !isEmpty()

    fun toIntArray(size1: Int, canReturnSelf: Boolean = true, exact: Boolean = true): IntArray {
        val values = values
        return if (canReturnSelf && (size1 == values.size || (!exact && size1 <= values.size))) {
            values
        } else {
            values.copyOf(size1)
        }
    }

    override fun destroy() {
        pool?.returnBuffer(values)
        size = 0
    }

    override fun toString(): String {
        val builder = StringBuilder(size * 4)
        builder.append('[')
        if (!isEmpty()) builder.append(this[0])
        for (i in 1 until size) {
            builder.append(',')
            builder.append(this[i])
        }
        builder.append(']')
        return builder.toString()
    }
}