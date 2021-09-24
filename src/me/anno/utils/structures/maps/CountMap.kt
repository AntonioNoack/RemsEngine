package me.anno.utils.structures.maps

open class CountMap<V> {

    private class Count(var value: Int = 0)

    private val store = HashMap<V, Count>()

    open fun getInitialCount(key: V): Int = 0

    operator fun get(key: V) = store[key]?.value ?: 0

    fun getAndInc(key: V): Int {
        val count = store.getOrPut(key) { Count(getInitialCount(key)) }
        return count.value++
    }

    fun incAndGet(key: V): Int {
        val count = store.getOrPut(key) { Count(getInitialCount(key)) }
        return ++count.value
    }

}