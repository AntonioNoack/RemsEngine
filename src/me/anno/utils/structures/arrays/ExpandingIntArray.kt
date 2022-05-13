package me.anno.utils.structures.arrays

import org.apache.logging.log4j.LogManager
import kotlin.math.max

class ExpandingIntArray(
    private val initCapacity: Int
) : List<Int> {

    override var size = 0

    fun clear() {
        size = 0
    }

    var array: IntArray? = null

    fun add(value: Int) = plusAssign(value)

    fun ensureCapacity(requestedSize: Int) {
        val array = array
        if (array == null || requestedSize >= array.size) {
            val suggestedSize = if (array == null) initCapacity else max(array.size * 2, 16)
            val newArray = IntArray(max(suggestedSize, requestedSize))
            if (array != null) System.arraycopy(array, 0, newArray, 0, this.size)
            this.array = newArray
        }
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

    override operator fun get(index: Int) = array!![index]

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

    override fun indexOf(element: Int): Int {
        val array = array ?: return -1
        for (i in 0 until size) {
            if (array[i] == element) return i
        }
        return -1
    }

    override fun lastIndexOf(element: Int): Int {
        val array = array ?: return -1
        for (i in size - 1 downTo 0) {
            if (array[i] == element) return i
        }
        return -1
    }

    override fun iterator(): Iterator<Int> {
        return listIterator()
    }

    override fun listIterator(): ListIterator<Int> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<Int> {
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

    override fun subList(fromIndex: Int, toIndex: Int): List<Int> {
        val array = array ?: return emptyList()
        return IntArray(toIndex - fromIndex) { array[fromIndex + it] }.toList()
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        for (e in elements) {
            if (!contains(e))
                return false
        }
        return true
    }

    fun toIntArray(): IntArray {
        val tmp = IntArray(size)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, size)
        return tmp
    }

    override fun toString(): String {
        val builder = StringBuilder(size * 4)
        builder.append('[')
        if (size > 0) builder.append(this[0])
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