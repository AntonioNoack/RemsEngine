package me.anno.utils.structures.maps

import me.anno.utils.structures.tuples.MutableTriple

/**
 * map, where each key is a triple
 * */
open class KeyTripleMap<KManifold, KFewOnly, KFewOnly2, Value>(capacity: Int = 16) :
    Iterable<List<MutableTriple<KFewOnly, KFewOnly2, Value>>> {

    val values = HashMap<KManifold, MutableList<MutableTriple<KFewOnly, KFewOnly2, Value>>>(capacity)

    operator fun get(
        k1: KManifold,
        k2: KFewOnly,
        k3: KFewOnly2
    ): Value? {
        return values[k1]?.firstOrNull { it.first == k2 && it.second == k3 }?.third
    }

    operator fun set(
        k1: KManifold,
        k2: KFewOnly,
        k3: KFewOnly2,
        v: Value
    ) {
        val list = values.getOrPut(k1) { ArrayList(8) }
        for (pairIndex in list.indices) {
            val pair = list[pairIndex]
            if (pair.first == k2 && pair.second == k3) {
                pair.third = v
                return
            }
        }
        list.add(MutableTriple(k2, k3, v))
    }

    inline fun getOrPut(
        k1: KManifold,
        k2: KFewOnly,
        k3: KFewOnly2,
        v: (k1: KManifold, k2: KFewOnly, k3: KFewOnly2) -> Value
    ): Value {
        val list = values.getOrPut(k1) { ArrayList(8) }
        for (pairIndex in list.indices) {
            val pair = list[pairIndex]
            if (pair.first == k2 && pair.second == k3) {
                return pair.third
            }
        }
        val value = v(k1, k2, k3)
        list.add(MutableTriple(k2, k3, value))
        return value
    }

    inline fun forEach(run: (k1: KManifold, k2: KFewOnly, k3: KFewOnly2, v: Value) -> Unit) {
        for ((k1, k2s) in values) {
            for ((k2, k3, v) in k2s) {
                run(k1, k2, k3, v)
            }
        }
    }

    override fun iterator(): Iterator<List<MutableTriple<KFewOnly, KFewOnly2, Value>>> {
        return values.values.iterator()
    }


}