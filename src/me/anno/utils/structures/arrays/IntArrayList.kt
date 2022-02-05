package me.anno.utils.structures.arrays

import kotlin.math.max
import kotlin.math.min

class IntArrayList(val capacity: Int) : List<Int> {

    private val buffers = ArrayList<IntArray>()

    override var size = 0
        private set

    override operator fun get(index: Int) = buffers[index / capacity][index % capacity]

    /** better: no conversion to Java object ... */
    fun getValue(index: Int) = buffers[index / capacity][index % capacity]

    operator fun plusAssign(value: Int) {
        val bufferIndex = size / capacity
        if (bufferIndex >= buffers.size) buffers.add(IntArray(capacity))
        buffers[bufferIndex][size % capacity] = value
        size++
    }

    fun add(value: Int) {
        plusAssign(value)
    }

    fun addAll(values: Collection<Int>) {
        for (v in values) add(v)
    }

    fun add(index: Int, value: Int) {
        // make space for element
        add(0)
        for (i in size - 2 downTo index) {
            set(i + 1, get(i))
        }
        set(index, value)
    }

    fun removeAt(index: Int) {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException()
        // copy elements from end to here, then reduce size
        size--
        for (i in index until size) {
            setUnsafe(i, get(i + 1))
        }
    }

    private fun setUnsafe(index: Int, element: Int) {
        buffers[index / capacity][index % capacity] = element
    }

    fun set(index: Int, element: Int) {
        val bufferIndex = index / capacity
        while (bufferIndex >= buffers.size) buffers.add(IntArray(capacity))
        buffers[bufferIndex][index % capacity] = element
        size = max(index + 1, size)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<Int> {
        if (fromIndex == 0 && toIndex == size) return this
        val list = ArrayList<Int>(toIndex - fromIndex)
        // could be optimized
        for (i in fromIndex until toIndex) list.add(get(i))
        return list
    }

    fun toList() = subList(0, size)

    fun toIntArray(): IntArray {
        val dst = IntArray(size)
        for (i in 0 until (size + capacity - 1) / capacity) {
            val src = buffers[i]
            val offset = i * capacity
            System.arraycopy(src, 0, dst, offset, min(capacity, size - offset))
        }
        return dst
    }

    fun clear() {
        size = 0
    }

    inline fun joinToString(
        separator: String = ",",
        prefix: String = "",
        suffix: String = "",
        transform: (Int) -> String
    ): String {
        val result = StringBuilder(prefix.length + suffix.length + size * 10)
        result.append(prefix)
        // could be optimized
        for (i in 0 until size) {
            if (i > 0) result.append(separator)
            result.append(transform(get(i)))
        }
        result.append(suffix)
        return result.toString()
    }

    fun joinChars(startIndex: Int = 0, endIndex: Int = size): CharSequence {
        val builder = StringBuilder(endIndex - startIndex)
        // could be optimized
        for (index in startIndex until endIndex) {
            builder.append(Character.toChars(get(index)))
        }
        return builder
    }

    override fun contains(element: Int): Boolean {
        for (index in 0 until size) {
            if (get(index) == element) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<Int>): Boolean {
        if (elements.isEmpty()) return true
        if (isEmpty()) return false
        val contained = elements.toHashSet()
        if (contained.size > size) return false
        for (i in 0 until size) {
            contained.remove(get(i))
            if (contained.isEmpty()) return true
        }
        return false
    }

    override fun indexOf(element: Int): Int {
        for (index in 0 until size) {
            if (get(index) == element) return index
        }
        return -1
    }

    override fun isEmpty() = size <= 0

    override fun iterator(): Iterator<Int> {
        return listIterator()
    }

    override fun lastIndexOf(element: Int): Int {
        for (index in size - 1 downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun listIterator(): ListIterator<Int> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<Int> {
        return object : ListIterator<Int> {
            private var idx = index
            override fun hasNext() = idx < size
            override fun hasPrevious() = idx > 0
            override fun next() = get(idx++)
            override fun nextIndex() = idx
            override fun previous() = get(--idx)
            override fun previousIndex() = idx - 1
        }
    }

}