package me.anno.utils.structures.arrays

import me.anno.cache.ICacheData
import me.anno.utils.pooling.IntArrayPool
import me.anno.utils.search.BinarySearch
import me.anno.utils.types.Strings.joinChars0
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

open class ExpandingIntArray(initCapacity: Int, val pool: IntArrayPool? = null) :
    Collection<Int>, ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(ExpandingIntArray::class)
    }

    override var size = 0

    var array = alloc(initCapacity)

    val capacity get() = array.size

    fun alloc(size: Int): IntArray {
        return if (pool != null) pool[size, true, false] else IntArray(size)
    }

    fun clear() {
        size = 0
    }

    fun add(value: Int) = plusAssign(value)

    fun ensureExtra(delta: Int) {
        ensureCapacity(size + delta)
    }

    fun ensureCapacity(requestedSize: Int) {
        val array = array
        if (requestedSize >= array.size) {
            val suggestedSize = max(array.size * 2, 16)
            val newSize = max(suggestedSize, requestedSize)
            val newArray = try {
                IntArray(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated ${newSize * 4L} bytes for ExpandingIntArray")
                throw e
            }
            array.copyInto(newArray)
            this.array = newArray
        }
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun inc(position: Int) {
        val array = array
        array[position]++
    }

    fun dec(position: Int) {
        val array = array
        array[position]--
    }

    fun add(index: Int, value: Int) {
        ensureCapacity(size + 1)
        val array = array
        array.copyInto(array, index + 1, index, size)
        array[index] = value
        size++
    }

    fun add(values: List<Int>, index0: Int = 0, index1: Int = values.size) {
        var size = size
        val length = index1 - index0
        ensureCapacity(size + length)
        val array = array
        for (index in index0 until index1) {
            array[size++] = values[index]
        }
        this.size = size
    }

    fun addUnsafe(src: IntArray, startIndex: Int = 0, length: Int = src.size - startIndex) {
        src.copyInto(array, size, startIndex, startIndex + length)
        size += length
    }

    @Suppress("unused")
    fun addUnsafe(src: ExpandingIntArray, startIndex: Int, length: Int) {
        addUnsafe(src.array, startIndex, length)
    }

    fun add(v: IntArray, srcStartIndex: Int = 0, length: Int = v.size - srcStartIndex) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun add(values: ExpandingIntArray, srcStartIndex: Int = 0, length: Int = values.size - srcStartIndex) {
        if (length < 0) throw IllegalArgumentException()
        if (length == 0) return
        ensureExtra(length)
        addUnsafe(values.array, srcStartIndex, length)
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
        val array = array
        val value = array[index]
        removeBetween(index, index + 1)
        return value
    }

    fun removeBetween(index0: Int, index1: Int) {
        val length = index1 - index0
        array.copyInto(array, index0, index1, size)
        size -= length
    }

    operator fun set(index: Int, value: Int) {
        array[index] = value
    }

    fun last() = array[size - 1]

    operator fun get(index: Int) = array[index]
    fun getOrNull(index: Int) = array.getOrNull(index)

    @Suppress("unused")
    fun addUnsafe(x: Int) {
        array[size++] = x
    }

    @Suppress("unused")
    fun addUnsafe(x: Int, y: Int) {
        val array = array
        var size = size
        array[size++] = x
        array[size++] = y
        this.size = size
    }

    fun add(x: Int, y: Int, z: Int) {
        ensureCapacity(size + 3)
        val array = array
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    @Suppress("unused")
    fun addUnsafe(x: Int, y: Int, z: Int) {
        val array = array
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    operator fun plusAssign(value: Int) {
        ensureCapacity(size + 1)
        array[size++] = value
    }

    override fun isEmpty(): Boolean = size <= 0

    override fun contains(element: Int): Boolean {
        return indexOf(element) >= 0
    }

    fun indexOf(element: Int): Int {
        val array = array
        for (i in indices) {
            if (array[i] == element) return i
        }
        return -1
    }

    fun lastIndexOf(element: Int): Int {
        val array = array
        for (i in size - 1 downTo 0) {
            if (array[i] == element) return i
        }
        return -1
    }

    override fun iterator() = listIterator()
    fun listIterator(index: Int = 0): ListIterator<Int> {
        val array = array
        return object : ListIterator<Int> {
            var i = index
            override fun hasNext(): Boolean = i < size
            override fun next(): Int = array[i++]
            override fun previous(): Int = array[--i]
            override fun nextIndex(): Int = i
            override fun previousIndex(): Int = i - 1
            override fun hasPrevious(): Boolean = i > 0
        }
    }

    fun subList(fromIndex: Int, toIndex: Int): List<Int> {
        val array = array
        return IntArray(toIndex - fromIndex) { array[fromIndex + it] }.toList()
    }

    @Suppress("unused")
    fun binarySearch(element: Int): Int {
        return BinarySearch.binarySearch(size) { index ->
            this[index].compareTo(element)
        }
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        for (e in elements) {
            if (!contains(e))
                return false
        }
        return true
    }


    fun toIntArray(canReturnSelf: Boolean = true, exact: Boolean = true) = toIntArray(size, canReturnSelf, exact)

    fun toIntArray(size1: Int, canReturnSelf: Boolean = true, exact: Boolean = true): IntArray {
        val array = array
        if (canReturnSelf && (size1 == array.size || (!exact && size1 <= array.size)))
            return array
        val value = alloc(size1)
        array.copyInto(value, 0, min(size, size1))
        return value
    }

    override fun destroy() {
        pool?.returnBuffer(array)
        size = 0
    }

    override fun toString(): String {
        val builder = StringBuilder(size * 4)
        builder.append('[')
        if (isNotEmpty()) builder.append(this[0])
        for (i in 1 until size) {
            builder.append(',')
            builder.append(this[i])
        }
        builder.append(']')
        return builder.toString()
    }
}