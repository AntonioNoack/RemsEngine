package me.anno.utils.structures.maps

import me.anno.utils.structures.lists.PairArrayList
import me.anno.utils.structures.maps.Maps.removeIf

/**
 * map, where each key is a pair
 * */
open class KeyPairMap<KManifold, KFewOnly, Value>(capacity: Int = 16) :
    Iterable<PairArrayList<KFewOnly, Value>> {

    val values = HashMap<KManifold, PairArrayList<KFewOnly, Value>>(capacity)
    var size = 0

    open fun clear() {
        values.clear()
        size = 0
    }

    open fun clear(k1: KManifold) {
        val vs = values[k1] ?: return
        size -= vs.size shr 1
        vs.clear()
    }

    fun addAll(other: KeyPairMap<KManifold, KFewOnly, Value>) {
        for ((k1, v) in other.values) {
            val base = values.getOrPut(k1) { PairArrayList(v.size) }
            if (base.size == 0) {
                for (i in 0 until v.size) {
                    base.add(v.getFirst(i), v.getSecond(i))
                }
                size += base.size
            } else {
                for (i in 0 until v.size) {
                    val a = v.getFirst(i)
                    val b = v.getSecond(i)
                    if (base.replaceOrAddMap(a, b)) size++
                }
            }
        }
    }

    operator fun get(k1: KManifold, k2: KFewOnly): Value? {
        return values[k1]?.findSecond(k2)
    }

    operator fun set(k1: KManifold, k2: KFewOnly, v: Value) {
        val list = values.getOrPut(k1) { PairArrayList(8) }
        if (list.replaceOrAddMap(k2, v)) size++
    }

    fun setUnsafe(k1: KManifold, k2: KFewOnly, v: Value) {
        val list = values.getOrPut(k1) { PairArrayList(8) }
        list.add(k2, v)
        size++
    }

    fun getOrPut(
        k1: KManifold,
        k2: KFewOnly,
        v: (k1: KManifold, k2: KFewOnly) -> Value
    ): Value {
        val list = values.getOrPut(k1) { PairArrayList(8) }
        val vs = list.mapFirstNotNull { k2s, vs -> if (k2s == k2) vs else null }
        if (vs != null) return vs
        val value = v(k1, k2)
        list.add(k2, value)
        size++
        return value
    }

    fun contains(k1: KManifold, k2: KFewOnly): Boolean {
        val list = values[k1] ?: return false
        return list.any { it.first == k2 }
    }

    fun <Result> map(mapping: (k1: KManifold, k2: KFewOnly, v: Value) -> Result): List<Result> {
        val result = ArrayList<Result>(size)
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                result.add(mapping(k1, k2, v))
            }
        }
        return result
    }

    fun <Result> mapNotNull(mapping: (k1: KManifold, k2: KFewOnly, v: Value) -> Result?): List<Result> {
        val result = ArrayList<Result>(size)
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                result.add(mapping(k1, k2, v) ?: continue)
            }
        }
        return result
    }

    fun forEach(callback: (k1: KManifold, k2: KFewOnly, v: Value) -> Unit) {
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                callback(k1, k2, v)
            }
        }
    }

    @Suppress("unused")
    fun replaceValues(run: (k1: KManifold, k2: KFewOnly, v: Value) -> Value): Int {
        var changed = 0
        for ((k1, k2s) in values) {
            changed += k2s.replaceSeconds { a, b -> run(k1, a, b) }
        }
        return changed
    }

    fun recalculateSize() {
        size = values.values.sumOf { it.size }
    }

    fun removeMajorIf(test: (k1: KManifold) -> Boolean): Boolean {
        return if (values.removeIf { test(it.key) } > 0) {
            recalculateSize()
            true
        } else false
    }

    fun removeIf(predicate: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean): Int {
        if (isEmpty()) return 0
        var removed = 0
        for ((k1, k2s) in values) {
            removed += k2s.removeIf { k2, v ->
                predicate(k1, k2, v)
            }
        }
        size -= removed
        return removed
    }

    fun remove(k1: KManifold, k2: KFewOnly): Boolean {
        val list = values[k1] ?: return false
        val delta = list.removeIf { first, _ -> first == k2 }
        size -= delta
        return delta > 0
    }

    fun count(predicate: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean): Int {
        var sum = 0
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                if (predicate(k1, k2, v)) sum++
            }
        }
        return sum
    }

    @Suppress("unused")
    fun countMajor(predicate: (k1: KManifold) -> Boolean): Int {
        var sum = 0
        for ((k1, k2s) in values) {
            if (predicate(k1)) sum += k2s.size
        }
        return sum
    }

    fun filterMajor(predicate: (k1: KManifold) -> Boolean) = values.filterKeys(predicate)

    override fun iterator() = values.values.iterator()

    fun isEmpty() = size <= 0
    fun isNotEmpty() = size > 0

    override fun toString(): String {
        val sb = StringBuilder(4 + size)
        sb.append("{")
        for ((i0, k1i) in values.entries.withIndex()) {
            val (k1, k2s) = k1i
            val s0 = sb.length
            sb.append(k1)
            if (sb.length == s0) sb.append("\"\"")
            sb.append(": { ")
            for ((i1, k2i) in k2s.withIndex()) {
                val (k2, v) = k2i
                val s1 = sb.length
                sb.append(k2)
                if (sb.length == s1) sb.append("\"\"")
                sb.append(": ")
                sb.append(v)
                sb.append(if (i1 == k2s.size - 1) " " else ", ")
            }
            sb.append(if (i0 == values.size - 1) "} " else "}, ")
        }
        sb.append("}")
        return sb.toString()
    }
}