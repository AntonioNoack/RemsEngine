package me.anno.utils.structures.lists

import me.anno.utils.structures.tuples.MutablePair
import kotlin.math.max

class PairArrayList<A, B>(capacity: Int = 16) : Iterable<MutablePair<A, B>> {

    var array = arrayOfNulls<Any>(max(capacity * 2, 2))
    var elementSize = 0

    val size get() = elementSize shr 1

    fun add(a: A, b: B) {
        if (elementSize + 2 >= array.size) {
            val newArray = arrayOfNulls<Any>(array.size * 2)
            System.arraycopy(array, 0, newArray, 0, elementSize)
            array = newArray
        }
        array[elementSize++] = a
        array[elementSize++] = b
    }

    fun byA(a: A): B? {
        for (i in 0 until elementSize step 2) {
            if (array[i] == a) return array[i + 1] as B
        }
        return null
    }

    fun byB(b: B): A? {
        for (i in 1 until elementSize step 2) {
            if (array[i] == b) return array[i - 1] as A
        }
        return null
    }

    fun removeAt(elementIndex: Int) {
        val size = elementSize
        if (size > elementIndex + 1) {
            // we can use the last one
            array[elementIndex + 1] = array[size - 1]
            array[elementIndex] = array[size - 2]
            elementSize -= 2
        } else {
            // we can just remove the two last
            elementSize -= 2
            // for the garbage collector
            array[elementIndex + 1] = null
            array[elementIndex] = null
        }
    }

    fun removeByA(a: A): Boolean {
        val size = elementSize
        for (i in 0 until size step 2) {
            if (array[i] == a) {
                removeAt(i)
                return true
            }
        }
        return false
    }

    fun remove(a: A, b: B): Boolean {
        val size = array.size
        for (i in 0 until size step 2) {
            if (array[i] == a && array[i + 1] == b) {
                removeAt(i)
                return true
            }
        }
        return false
    }

    fun replaceOrAddMap(a: A, b: B) {
        for (i in 0 until elementSize step 2) {
            if (array[i] == a) {
                array[i + 1] = b
                return
            }
        }
        add(a, b)
    }

    override fun iterator(): Iterator<MutablePair<A, B>> {
        return object : Iterator<MutablePair<A, B>> {
            val pair = MutablePair(null as A, null as B)
            var index = 0
            override fun hasNext(): Boolean = index < elementSize
            override fun next(): MutablePair<A, B> {
                pair.first = array[index++] as A
                pair.second = array[index++] as B
                return pair
            }
        }
    }

    fun removeIf(run: (a: A, b: B) -> Boolean): Boolean {
        var result = false
        var i = 0
        while (i < elementSize) {
            if (run(array[i] as A, array[i + 1] as B)) {
                removeAt(i)
                result = true
            } else i += 2
        }
        return result
    }

}