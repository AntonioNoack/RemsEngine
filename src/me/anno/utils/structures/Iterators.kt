package me.anno.utils.structures

import me.anno.io.zip.NextEntryIterator

object Iterators {

    inline fun <V> Iterator<V>.firstOrNull(test: (V) -> Boolean): V? {
        while (hasNext()) {
            val element = next()
            if (test(element)) return element
        }
        return null
    }

    fun <V> Iterator<V>.count(): Int {
        var ctr = 0
        while (hasNext()) {
            next()
            ctr++
        }
        return ctr
    }

    inline fun <V> Iterator<V>.count(test: (V) -> Boolean): Int {
        var ctr = 0
        while (hasNext()) {
            if (test(next())) ctr++
        }
        return ctr
    }

    fun <V> Iterator<V>.filter(test: (V) -> Boolean): Iterator<V> {
        val base = this
        return object : NextEntryIterator<V>() {
            override fun nextEntry(): V? {
                while (base.hasNext()) {
                    val next = base.next()
                    if (test(next)) return next
                }
                return null
            }
        }
    }

    fun <A, B> Iterator<A>.map(mapping: (A) -> B): Iterator<B> {
        val base = this
        return object : Iterator<B> {
            override fun hasNext() = base.hasNext()
            override fun next() = mapping(base.next())
        }
    }

    fun <V> Iterator<V>.toList(): List<V> {
        val list = ArrayList<V>()
        while (hasNext()) {
            list.add(next())
        }
        return list
    }

    fun <V> Iterator<V>.subList(startIndex: Int, endIndex: Int): List<V> {
        for (i in 0 until startIndex) {
            if (hasNext()) next()
            else return emptyList()
        }
        val length = endIndex - startIndex
        val list = ArrayList<V>(length)
        while (list.size < length && hasNext()) {
            list.add(next())
        }
        return list
    }

}