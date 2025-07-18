package me.anno.utils.structures.arrays

import me.anno.cache.ICacheData
import me.anno.utils.pooling.IntArrayPool
import me.anno.utils.search.BinarySearch

open class IntArrayList(initCapacity: Int = 16, val pool: IntArrayPool? = null) :
    NativeArrayList, ICacheData {

    constructor(values: IntArray) : this(values.size) {
        add(values)
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

    fun last(): Int = values[size - 1]

    operator fun get(index: Int): Int = values[index]

    fun getOrNull(index: Int): Int? {
        return if (index in indices) this[index] else null
    }

    fun getOrDefault(index: Int, default: Int): Int {
        return if (index in indices) this[index] else default
    }

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

    operator fun contains(element: Int): Boolean {
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

    fun <V> map(mapping: (Int) -> V): List<V> {
        val dst = ArrayList<V>(size)
        for (i in 0 until size) {
            dst.add(mapping(get(i)))
        }
        return dst
    }

    fun <V> map(mapping: List<V>): List<V> {
        val dst = ArrayList<V>(size)
        for (i in 0 until size) {
            dst.add(mapping[get(i)])
        }
        return dst
    }

    fun toIntArray(size1: Int, canReturnSelf: Boolean = true, exact: Boolean = true): IntArray {
        val values = values
        return if (canReturnSelf && (size1 == values.size || (!exact && size1 <= values.size))) {
            values
        } else {
            values.copyOf(size1)
        }
    }

    fun reverse() {
        val li = size - 1
        val values = values
        for (i in 0 until size.shr(1)) {
            val j = li - i
            val tmp = values[i]
            values[i] = values[j]
            values[j] = tmp
        }
    }

    fun swap(i: Int, j: Int) {
        val tmp = values[i]
        values[i] = values[j]
        values[j] = tmp
    }

    fun fill(value: Int) {
        values.fill(value, 0, size)
    }

    fun sum(): Long {
        var sum = 0L
        val values = values
        for (i in 0 until size) {
            sum += values[i]
        }
        return sum
    }

    fun sort() {
        values.sort(0, size)
    }

    fun removeDuplicatesSorted() {
        if (size < 2) return // already unique
        var writeIndex = 1
        val values = values
        var lastValue = values[0]
        for (readIndex in 1 until size) {
            val v = values[readIndex]
            if (v != lastValue) {
                values[writeIndex++] = v
                lastValue = v
            }
        }
        size = writeIndex
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

    override fun equals(other: Any?): Boolean {
        return other is IntArrayList &&
                other.size == size &&
                (0 until size).all {
                    other[it] == this[it]
                }
    }

    override fun hashCode(): Int {
        var result = 1
        for (i in 0 until size) {
            result = 31 * result + this[i]
        }
        return result
    }
}