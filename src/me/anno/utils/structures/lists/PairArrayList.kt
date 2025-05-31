package me.anno.utils.structures.lists

import me.anno.utils.structures.tuples.MutablePair
import org.apache.logging.log4j.LogManager
import kotlin.math.max

@Suppress("unused")
class PairArrayList<First, Second>(capacity: Int = 16) : Iterable<MutablePair<First, Second>> {

    var array = arrayOfNulls<Any>(max(capacity * 2, 2))

    /**
     * = size * 2
     * */
    var elementSize = 0

    val size get() = elementSize shr 1

    fun clear() {
        elementSize = 0
        array.fill(null)
    }

    fun isEmpty(): Boolean = elementSize <= 0
    fun isNotEmpty(): Boolean = elementSize > 0

    @Suppress("unchecked_cast")
    fun getFirst(index: Int): First = array[index * 2] as First

    @Suppress("unchecked_cast")
    fun getSecond(index: Int): Second = array[index * 2 + 1] as Second

    @Suppress("unchecked_cast")
    fun setFirst(index: Int, first: First): First? {
        val i = index * 2
        val array = array
        val prev = array[i]
        array[i] = first
        return prev as First
    }

    @Suppress("unchecked_cast")
    fun setSecond(index: Int, second: Second): Second? {
        val i = index * 2 + 1
        val array = array
        val prev = array[i]
        array[i] = second
        return prev as Second
    }

    fun lastFirst() = getFirst(size - 1)
    fun lastSecond() = getSecond(size - 1)

    fun removeLast() {
        elementSize -= 2
    }

    inline fun <V> mapFirstNotNull(mapEntry: (a: First, b: Second) -> V?): V? {
        for (i in 0 until size) {
            val v = mapEntry(getFirst(i), getSecond(i))
            if (v != null) return v
        }
        return null
    }

    fun add(a: First, b: Second) {
        var elementSize = elementSize
        var array = array
        if (elementSize + 2 >= array.size) {
            val newArray = array.copyOf(array.size * 2)
            this.array = newArray
            array = newArray
        }
        array[elementSize++] = a
        array[elementSize++] = b
        this.elementSize = elementSize
    }

    fun indexOf(first: First, second: Second): Int {
        for (i in 0 until size) {
            if (getFirst(i) == first && getSecond(i) == second) return i
        }
        return -1
    }

    fun indexOfFirst(first: First): Int {
        for (i in 0 until size) {
            if (getFirst(i) == first) return i
        }
        return -1
    }

    fun indexOfSecond(second: Second): Int {
        for (i in 0 until size) {
            if (getSecond(i) == second) return i
        }
        return -1
    }

    fun findSecond(first: First): Second? {
        val i = indexOfFirst(first)
        return if (i >= 0) getSecond(i) else null
    }

    fun findFirst(second: Second): First? {
        val i = indexOfSecond(second)
        return if (i >= 0) getFirst(i) else null
    }

    fun removeAt(index: Int, keepOrder: Boolean): Boolean {
        if (index !in 0 until size) {
            return false
        }
        val array = array
        val elementIndex = index.shl(1)
        if (keepOrder) { // O(n)
            array.copyInto(array, elementIndex, elementIndex + 2, elementSize)
        } else if (index + 1 < size) { // O(1)
            array[elementIndex] = array[elementSize - 2]
            array[elementIndex + 1] = array[elementSize - 1]
        } // else done
        elementSize -= 2
        return true
    }

    fun removeByFirst(first: First, keepOrder: Boolean): Boolean {
        return removeAt(indexOfFirst(first), keepOrder)
    }

    fun removeBySecond(second: Second, keepOrder: Boolean): Boolean {
        return removeAt(indexOfSecond(second), keepOrder)
    }

    fun remove(first: First, second: Second, keepOrder: Boolean): Boolean {
        return removeAt(indexOf(first, second), keepOrder)
    }

    /**
     * @return whether an element was added
     * */
    fun replaceOrAddMap(first: First, second: Second): Boolean {
        val i = indexOfFirst(first)
        if (i >= 0) {
            array[i * 2 + 1] = second
        } else {
            add(first, second)
        }
        return i < 0
    }

    fun replaceSeconds(transform: (a: First, b: Second) -> Second): Int {
        var changed = 0
        for (i in 0 until size) {
            val oldValue = getSecond(i)
            val newValue = transform(getFirst(i), oldValue)
            if (newValue !== oldValue) {
                array[i * 2 + 1] = newValue
                changed++
            }
        }
        return changed
    }

    override fun iterator(): Iterator<MutablePair<First, Second>> {
        return object : Iterator<MutablePair<First, Second>> {
            @Suppress("unchecked_cast")
            val pair = MutablePair(null as First, null as Second)
            var index = 0
            override fun hasNext(): Boolean = index < elementSize

            @Suppress("unchecked_cast")
            override fun next(): MutablePair<First, Second> {
                pair.first = array[index++] as First
                pair.second = array[index++] as Second
                return pair
            }
        }
    }

    /**
     * @return how many elements were removed
     * */
    fun removeIf(shouldRemove: (first: First, second: Second) -> Boolean): Int {
        var writeIndex = 0
        val array = array
        val size = size
        for (readIndex in 0 until size) {
            val first = getFirst(readIndex)
            val second = getSecond(readIndex)
            if (!shouldRemove(first, second)) {
                array[writeIndex++] = first
                array[writeIndex++] = second
            }
        }
        array.fill(null, writeIndex, size * 2) // clear rest for GC
        elementSize = writeIndex
        return size - writeIndex.shr(1)
    }

    override fun toString(): String {
        return (0 until size)
            .joinToString { "(${getFirst(it)}, ${getSecond(it)})" }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PairArrayList::class)
    }
}