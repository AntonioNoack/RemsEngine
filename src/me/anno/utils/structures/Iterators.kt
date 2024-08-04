package me.anno.utils.structures

import me.anno.utils.assertions.assertTrue

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
        return object : Iterator<V> {
            private var foundNext: Any? = null
            private var hasFoundNext = false
            override fun hasNext(): Boolean {
                if (hasFoundNext) return true
                while (base.hasNext()) {
                    val next = base.next()
                    if (test(next)) {
                        foundNext = next
                        hasFoundNext = true
                        break
                    }
                }
                return hasFoundNext
            }

            override fun next(): V {
                assertTrue(hasNext())
                hasFoundNext = false
                @Suppress("UNCHECKED_CAST")
                return foundNext as V
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

    fun <A, B : Any> Iterator<A>.mapNotNull(mapping: (A) -> B?): Iterator<B> {
        @Suppress("UNCHECKED_CAST")
        return map(mapping).filter { it != null } as Iterator<B>
    }

    fun <V> Iterator<V>.toList(): List<V> {
        val list = ArrayList<V>()
        while (hasNext()) {
            list.add(next())
        }
        return list
    }

    fun <V> Iterator<V>.skip(n: Int): Iterator<V> {
        for (i in 0 until n) {
            if (hasNext()) next()
            else break
        }
        return this
    }

    fun <V> Iterator<V>.take(length: Int): ArrayList<V> {
        val list = ArrayList<V>(length)
        while (list.size < length && hasNext()) {
            list.add(next())
        }
        return list
    }

    fun <V> Iterator<V>.subList(startIndex: Int, endIndex: Int): ArrayList<V> {
        return skip(startIndex).take(endIndex - startIndex)
    }

    fun <V> Iterator<V>?.then(other: Iterator<V>?): Iterator<V>? {
        val self = this
        if (self == null || !self.hasNext()) return other
        if (other == null) return this
        return object : Iterator<V> {
            private var useOther = false
            override fun hasNext(): Boolean {
                return self.hasNext() || other.hasNext()
            }

            override fun next(): V {
                useOther = useOther || !self.hasNext()
                return if (useOther) other.next()
                else self.next()
            }
        }
    }
}