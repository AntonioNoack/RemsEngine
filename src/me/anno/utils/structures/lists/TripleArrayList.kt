package me.anno.utils.structures.lists

import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.tuples.MutableTriple
import org.apache.logging.log4j.LogManager
import kotlin.math.max

@Suppress("unused")
class TripleArrayList<First, Second, Third>(capacity: Int = 16) : Iterable<MutableTriple<First, Second, Third>> {

    var array = arrayOfNulls<Any>(max(capacity * 3, 12))

    /**
     * = size * 2
     * */
    var elementSize = 0

    val size get() = elementSize / 3

    fun clear() {
        elementSize = 0
        array.fill(null)
    }

    fun isEmpty(): Boolean = elementSize <= 0
    fun isNotEmpty(): Boolean = elementSize > 0

    @Suppress("unchecked_cast")
    fun getFirst(index: Int): First = array[index * 3] as First

    @Suppress("unchecked_cast")
    fun getSecond(index: Int): Second = array[index * 3 + 1] as Second

    @Suppress("unchecked_cast")
    fun getThird(index: Int): Third = array[index * 3 + 2] as Third

    fun setFirst(index: Int, first: First): First {
        val i = index * 3
        val array = array

        @Suppress("unchecked_cast")
        val prev = array[i] as First
        array[i] = first
        return prev
    }

    fun setSecond(index: Int, second: Second): Second {
        val i = index * 3 + 1
        val array = array

        @Suppress("unchecked_cast")
        val prev = array[i] as Second
        array[i] = second
        return prev
    }

    fun setThird(index: Int, third: Third): Third {
        val i = index * 3 + 2
        val array = array

        @Suppress("unchecked_cast")
        val prev = array[i] as Third
        array[i] = third
        return prev
    }

    fun lastFirst() = getFirst(size - 1)
    fun lastSecond() = getSecond(size - 1)
    fun lastThird() = getThird(size - 1)

    fun removeLast() {
        elementSize -= 3
    }

    inline fun <V> mapFirstNotNull(mapEntry: (a: First, b: Second) -> V?): V? {
        for (i in 0 until size) {
            val v = mapEntry(getFirst(i), getSecond(i))
            if (v != null) return v
        }
        return null
    }

    fun add(first: First, second: Second, third: Third) {
        var elementSize = elementSize
        var array = array
        if (elementSize + 3 >= array.size) {
            val newArray = array.copyOf(array.size * 2)
            this.array = newArray
            array = newArray
        }
        array[elementSize++] = first
        array[elementSize++] = second
        array[elementSize++] = third
        this.elementSize = elementSize
    }

    fun indexOf(first: First, second: Second, third: Third): Int {
        for (i in 0 until size) {
            if (getFirst(i) == first &&
                getSecond(i) == second &&
                getThird(i) == third
            ) return i
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

    fun indexOfThird(third: Third): Int {
        for (i in 0 until size) {
            if (getThird(i) == third) return i
        }
        return -1
    }

    fun removeAt(index: Int, keepOrder: Boolean): Boolean {
        if (index !in 0 until size) {
            return false
        }
        val array = array
        val elementIndex = index * 3
        if (keepOrder) { // O(n)
            array.copyInto(array, elementIndex, elementIndex + 3, elementSize)
        } else if (index + 1 < size) { // O(1)
            array[elementIndex] = array[elementSize - 3]
            array[elementIndex + 1] = array[elementSize - 2]
            array[elementIndex + 2] = array[elementSize - 1]
        } // else done
        elementSize -= 3
        return true
    }

    fun remove(first: First, second: Second, third: Third, keepOrder: Boolean): Boolean {
        return removeAt(indexOf(first, second, third), keepOrder)
    }

    override fun iterator(): Iterator<MutableTriple<First, Second, Third>> {
        return object : Iterator<MutableTriple<First, Second, Third>> {
            @Suppress("unchecked_cast")
            val triple = MutableTriple(null as First, null as Second, null as Third)
            var index = 0
            override fun hasNext(): Boolean = index < elementSize

            @Suppress("unchecked_cast")
            override fun next(): MutableTriple<First, Second, Third> {
                val array = array
                triple.first = array[index++] as First
                triple.second = array[index++] as Second
                triple.third = array[index++] as Third
                return triple
            }
        }
    }

    /**
     * @return how many elements were removed
     * */
    fun removeIf(shouldRemove: (first: First, second: Second, third: Third) -> Boolean): Int {
        var writeIndex = 0
        val array = array
        val size = size
        for (i in 0 until size) {
            val first = getFirst(i)
            val second = getSecond(i)
            val third = getThird(i)
            if (!shouldRemove(first, second, third)) {
                setFirst(writeIndex, first)
                setSecond(writeIndex, second)
                setThird(writeIndex, third)
                writeIndex++
            }
        }
        array.fill(null, writeIndex * 3, elementSize) // clear rest for GC
        elementSize = writeIndex * 3
        return size - writeIndex
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append('[')
        val array = array
        val size = elementSize
        forLoopSafely(size, 3) { readIndex ->
            builder.append(if (readIndex > 0) ", (" else "(")
            builder.append(array[readIndex]).append(", ")
            builder.append(array[readIndex + 1]).append(", ")
            builder.append(array[readIndex + 2]).append(')')
        }
        builder.append(']')
        return builder.toString()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(TripleArrayList::class)
    }
}