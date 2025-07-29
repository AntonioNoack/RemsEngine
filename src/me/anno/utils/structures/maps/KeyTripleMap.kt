package me.anno.utils.structures.maps

import me.anno.utils.structures.lists.TripleArrayList

/**
 * map, where each key is a triple
 * */
open class KeyTripleMap<KManifold, KFewOnly, KFewOnly2, Value>(capacity: Int = 16) :
    Iterable<TripleArrayList<KFewOnly, KFewOnly2, Value>> {

    val values = HashMap<KManifold, TripleArrayList<KFewOnly, KFewOnly2, Value>>(capacity)

    operator fun get(k1: KManifold, k2: KFewOnly, k3: KFewOnly2): Value? {
        return values[k1]?.firstOrNull { it.first == k2 && it.second == k3 }?.third
    }

    operator fun set(k1: KManifold, k2: KFewOnly, k3: KFewOnly2, v: Value) {
        val list = values.getOrPut(k1) { TripleArrayList(8) }
        for (i in 0 until list.size) {
            if (list.getFirst(i) == k2 && list.getSecond(i) == k3) {
                list.setThird(i, v)
                return
            }
        }
        list.add(k2, k3, v)
    }

    fun getOrPut(
        k1: KManifold, k2: KFewOnly, k3: KFewOnly2,
        v: (k1: KManifold, k2: KFewOnly, k3: KFewOnly2) -> Value
    ): Value {
        val list = values.getOrPut(k1) { TripleArrayList(8) }
        for (i in 0 until list.size) {
            if (list.getFirst(i) == k2 && list.getSecond(i) == k3) {
                return list.getThird(i)
            }
        }
        val value = v(k1, k2, k3)
        list.add(k2, k3, value)
        return value
    }

    inline fun forEach(callback: (k1: KManifold, k2: KFewOnly, k3: KFewOnly2, v: Value) -> Unit) {
        for ((k1, k2s) in values) {
            for ((k2, k3, v) in k2s) {
                callback(k1, k2, k3, v)
            }
        }
    }

    override fun iterator() = values.values.iterator()

    fun clear() {
        values.clear()
    }
}