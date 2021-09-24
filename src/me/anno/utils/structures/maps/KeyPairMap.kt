package me.anno.utils.structures.maps

import me.anno.utils.structures.tuples.MutablePair

class KeyPairMap<KManifold, KFewOnly, Value>(capacity: Int = 16) :
    Iterable<List<MutablePair<KFewOnly, Value>>> {

    val values = HashMap<KManifold, MutableList<MutablePair<KFewOnly, Value>>>(capacity)
    var size = 0

    operator fun get(
        k1: KManifold,
        k2: KFewOnly
    ): Value? {
        return values[k1]?.firstOrNull { it.first == k2 }?.second
    }

    operator fun set(
        k1: KManifold,
        k2: KFewOnly,
        v: Value
    ) {
        val list = values.getOrPut(k1) { ArrayList(8) }
        for (pair in list) {
            if (pair.first == k2) {
                pair.second = v
                size++
                return
            }
        }
        list.add(MutablePair(k2, v))
        size++
    }

    inline fun getOrPut(
        k1: KManifold,
        k2: KFewOnly,
        v: (k1: KManifold, k2: KFewOnly) -> Value
    ): Value {
        val list = values.getOrPut(k1) { ArrayList(8) }
        for (pair in list) {
            if (pair.first == k2) {
                return pair.second
            }
        }
        val value = v(k1, k2)
        list.add(MutablePair(k2, value))
        size++
        return value
    }

    fun contains(k1: KManifold, k2: KFewOnly): Boolean {
        val list = values[k1] ?: return false
        return list.any { it.first == k2 }
    }

    inline fun <Result> map(run: (k1: KManifold, k2: KFewOnly, v: Value) -> Result): List<Result> {
        val result = ArrayList<Result>(size)
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                result.add(run(k1, k2, v))
            }
        }
        return result
    }

    inline fun <Result> mapNotNull(run: (k1: KManifold, k2: KFewOnly, v: Value) -> Result?): List<Result> {
        val result = ArrayList<Result>(size)
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                result.add(run(k1, k2, v) ?: continue)
            }
        }
        return result
    }

    inline fun forEach(run: (k1: KManifold, k2: KFewOnly, v: Value) -> Unit) {
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                run(k1, k2, v)
            }
        }
    }

    inline fun removeMajorIf(noinline run: (k1: KManifold) -> Boolean): Boolean {
        return if (values.keys.removeIf(run)) {
            size = values.values.sumOf { it.size }
            true
        } else false
    }

    inline fun removeIf(crossinline run: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean) {
        for ((k1, k2s) in values) {
            k2s.removeIf { (k2, v) ->
                run(k1, k2, v)
            }
        }
        size = values.values.sumOf { it.size }
    }

    fun remove(k1: KManifold, k2: KFewOnly): Boolean {
        val list = values[k1] ?: return false
        return if(list.removeIf { it.first == k2 }){
            size--
            true
        } else false
    }

    fun count(run: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean): Int {
        var sum = 0
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                if (run(k1, k2, v)) sum++
            }
        }
        return sum
    }

    override fun iterator(): Iterator<List<MutablePair<KFewOnly, Value>>> {
        return values.values.iterator()
    }

    fun isEmpty() = size == 0


}