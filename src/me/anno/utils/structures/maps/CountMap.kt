package me.anno.utils.structures.maps

open class CountMap<V>(capacity: Int = 16) {

    class Count(var value: Int = 0)

    val values = HashMap<V, Count>(capacity)

    open fun getInitialCount(key: V): Int = 0

    operator fun get(key: V) = values[key]?.value ?: 0

    fun getAndInc(key: V): Int {
        val count = values.getOrPut(key) { Count(getInitialCount(key)) }
        return count.value++
    }

    @Suppress("unused")
    fun incAndGet(key: V): Int {
        return getAndInc(key) + 1
    }
}