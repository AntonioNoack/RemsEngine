package me.anno.utils.structures.maps

/**
 * set with cardinalities
 * */
open class CountMap<V>(capacity: Int = 16) {

    class Count(var value: Int = 0)

    val values = HashMap<V, Count>(capacity)

    open fun getInitialCount(key: V): Int = 0

    operator fun get(key: V) = values[key]?.value ?: 0

    fun getAndInc(key: V, delta: Int = 1): Int {
        return incAndGet(key, delta) - delta
    }

    fun incAndGet(key: V, delta: Int = 1): Int {
        val count = values.getOrPut(key) { Count(getInitialCount(key)) }
        count.value += delta
        return count.value
    }

    fun clear() {
        values.clear()
    }
}