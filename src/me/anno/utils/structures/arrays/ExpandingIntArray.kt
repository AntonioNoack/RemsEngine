package me.anno.utils.structures.arrays

import me.anno.utils.LOGGER
import me.anno.utils.search.BinarySearch
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

open class ExpandingIntArray(private val initCapacity: Int) : Collection<Int> {

    override var size = 0

    fun clear() {
        size = 0
    }

    var array: IntArray? = null

    fun add(value: Int) = plusAssign(value)

    fun ensureExtra(delta: Int) {
        ensureCapacity(size + delta)
    }

    fun ensureCapacity(requestedSize: Int) {
        val array = array
        if (array == null || requestedSize >= array.size) {
            val suggestedSize = if (array == null) initCapacity else max(array.size * 2, 16)
            val newSize = max(suggestedSize, requestedSize)
            val newArray = try {
                IntArray(newSize)
            } catch (e: OutOfMemoryError) {
                LOGGER.warn("Failed to allocated ${newSize * 4L} bytes for ExpandingIntArray")
                throw e
            }
            if (array != null) System.arraycopy(array, 0, newArray, 0, this.size)
            this.array = newArray
        }
    }

    fun skip(delta: Int) {
        ensureExtra(delta)
        size += delta
    }

    fun inc(position: Int) {
        val array = array ?: return
        array[position]++
    }

    fun dec(position: Int) {
        val array = array ?: return
        array[position]--
    }

    fun add(index: Int, value: Int) {
        ensureCapacity(size + 1)
        val array = array!!
        System.arraycopy(array, index, array, index + 1, size - index)
        array[index] = value
        size++
    }

    fun add(values: List<Int>, index0: Int = 0, index1: Int = values.size) {
        var size = size
        val length = index1 - index0
        ensureCapacity(size + length)
        val array = array!!
        for (index in index0 until index1) {
            array[size++] = values[index]
        }
        this.size = size
    }

    fun addUnsafe(src: IntArray, startIndex: Int = 0, length: Int = src.size - startIndex) {
        System.arraycopy(src, startIndex, array!!, size, length)
        size += length
    }

    fun addUnsafe(src: ExpandingIntArray, startIndex: Int, length: Int) {
        System.arraycopy(src.array!!, startIndex, array!!, size, length)
        size += length
    }

    fun add(v: IntArray, srcStartIndex: Int = 0, length: Int = v.size - srcStartIndex) {
        ensureExtra(length)
        addUnsafe(v, srcStartIndex, length)
    }

    fun add(values: ExpandingIntArray, srcStartIndex: Int = 0, length: Int = values.size - srcStartIndex) {
        ensureCapacity(size + length)
        val dst = array!!
        val src = values.array!!
        System.arraycopy(src, srcStartIndex, dst, size, length)
        this.size += length
    }

    fun joinChars(startIndex: Int = 0, endIndex: Int = size): CharSequence {
        val builder = StringBuilder(endIndex - startIndex)
        // could be optimized
        for (index in startIndex until endIndex) {
            builder.append(Character.toChars(get(index)))
        }
        return builder
    }

    fun removeAt(index: Int) {
        val array = array ?: return
        System.arraycopy(array, index + 1, array, index, size - index - 1)
        size--
    }

    fun removeBetween(index0: Int, index1: Int) {
        val length = index1 - index0
        val array = array ?: return
        System.arraycopy(array, index1, array, index0, size - index1)
        size -= length
    }

    operator fun set(index: Int, value: Int) {
        array!![index] = value
    }

    fun last() = array!![size - 1]

    operator fun get(index: Int) = array!![index]

    fun addUnsafe(x: Int) {
        array!![size++] = x
    }

    fun addUnsafe(x: Int, y: Int) {
        val array = array!!
        var size = size
        array[size++] = x
        array[size++] = y
        this.size = size
    }

    fun add(x: Int, y: Int, z: Int) {
        ensureCapacity(size + 3)
        val array = array!!
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    fun addUnsafe(x: Int, y: Int, z: Int) {
        val array = array!!
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    operator fun plusAssign(value: Int) {
        ensureCapacity(size + 1)
        array!![size++] = value
    }

    override fun isEmpty(): Boolean = size <= 0

    override fun contains(element: Int): Boolean {
        return indexOf(element) >= 0
    }

    fun indexOf(element: Int): Int {
        val array = array ?: return -1
        for (i in indices) {
            if (array[i] == element) return i
        }
        return -1
    }

    fun lastIndexOf(element: Int): Int {
        val array = array ?: return -1
        for (i in size - 1 downTo 0) {
            if (array[i] == element) return i
        }
        return -1
    }

    override fun iterator() = listIterator()
    fun listIterator(index: Int = 0): ListIterator<Int> {
        val array = array!!
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
        val array = array ?: return emptyList()
        return IntArray(toIndex - fromIndex) { array[fromIndex + it] }.toList()
    }

    fun binarySearch(element: Int): Int {
        return BinarySearch.binarySearch(size) {
            it.compareTo(element)
        }
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        for (e in elements) {
            if (!contains(e))
                return false
        }
        return true
    }

    fun toIntArray(size1: Int): IntArray {
        val tmp = IntArray(size1)
        if (isNotEmpty()) System.arraycopy(array!!, 0, tmp, 0, min(size, size1))
        return tmp
    }

    fun toIntArray(): IntArray {
        val tmp = IntArray(size)
        if (isNotEmpty()) System.arraycopy(array!!, 0, tmp, 0, size)
        return tmp
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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val logger = LogManager.getLogger(ExpandingIntArray::class)
            val list = ExpandingIntArray(16)
            list.add(1)
            list.add(2)
            list.add(3)
            logger.info(list) // 1,2,3
            list.removeAt(1)
            logger.info(list) // 1,3
            list.add(0, 5)
            logger.info(list) // 5,1,3
            list.removeBetween(0, 1)
            logger.info(list) // 1,3
        }
    }

}