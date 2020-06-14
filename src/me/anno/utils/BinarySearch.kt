package me.anno.utils

import java.lang.RuntimeException

object BinarySearch {

    class ExpensiveList<V: Comparable<V>>(val length: Int, val generator: (Int) -> V): List<V> {

        val cache = arrayOfNulls<Any>(length)

        override fun isEmpty() = length == 0

        override val size: Int
            get() = length

        override fun get(index: Int): V {
            var v = cache[index]
            if(v == null){
                v = generator(index)
                cache[index] = v
            }
            return v as V
        }

        // it's expensive; no time for that
        override fun iterator() = throw RuntimeException()
        override fun listIterator(index: Int) = throw RuntimeException()
        override fun indexOf(element: V) = throw RuntimeException()
        override fun lastIndexOf(element: V) = throw RuntimeException()
        override fun listIterator() = throw RuntimeException()
        override fun contains(element: V) = throw RuntimeException()
        override fun containsAll(elements: Collection<V>) = throw RuntimeException()
        // too lazy to implement that
        override fun subList(fromIndex: Int, toIndex: Int) = throw RuntimeException()
    }

}