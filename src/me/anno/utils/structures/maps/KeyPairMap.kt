package me.anno.utils.structures.maps

import me.anno.utils.structures.tuples.MutablePair

class KeyPairMap<KManifold, KFewOnly, Value>(capacity: Int = 16) : Iterable<List<MutablePair<KFewOnly, Value>>> {

    val values = HashMap<KManifold, MutableList<MutablePair<KFewOnly, Value>>>(capacity)

    operator fun get(k1: KManifold, k2: KFewOnly): Value? {
        return values[k1]?.firstOrNull { it.first == k2 }?.second
    }

    operator fun set(k1: KManifold, k2: KFewOnly, v: Value) {
        val list = values.getOrPut(k1) { ArrayList(8) }
        for (pair in list) {
            if (pair.first == k2) {
                pair.second = v
                return
            }
        }
        list.add(MutablePair(k2, v))
    }

    inline fun getOrPut(k1: KManifold, k2: KFewOnly, v: (k1: KManifold, k2: KFewOnly) -> Value): Value {
        val list = values.getOrPut(k1) { ArrayList(8) }
        for (pair in list) {
            if (pair.first == k2) {
                return pair.second
            }
        }
        val value = v(k1, k2)
        list.add(MutablePair(k2, value))
        return value
    }

    override fun iterator(): Iterator<List<MutablePair<KFewOnly, Value>>> {
        return values.values.iterator()
    }


}