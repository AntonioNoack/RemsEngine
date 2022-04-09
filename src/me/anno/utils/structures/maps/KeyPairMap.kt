package me.anno.utils.structures.maps

import me.anno.utils.structures.lists.PairArrayList

class KeyPairMap<KManifold, KFewOnly, Value>(capacity: Int = 16) :
    Iterable<PairArrayList<KFewOnly, Value>> {

    val values = HashMap<KManifold, PairArrayList<KFewOnly, Value>>(capacity)
    var size = 0

    fun clear() {
        values.clear()
        size = 0
    }

    operator fun get(
        k1: KManifold,
        k2: KFewOnly
    ): Value? {
        return values[k1]?.byA(k2)
    }

    operator fun set(
        k1: KManifold,
        k2: KFewOnly,
        v: Value
    ) {
        // if (k1 == Path.ROOT_PATH && k2 == "gravity") throw RuntimeException()
        // LOGGER.info("('$k1','$k2') = '$v'")
        val list = values.getOrPut(k1) { PairArrayList(8) }
        if (list.replaceOrAddMap(k2, v)) size++
    }

    inline fun getOrPut(
        k1: KManifold,
        k2: KFewOnly,
        v: (k1: KManifold, k2: KFewOnly) -> Value
    ): Value {
        val list = values.getOrPut(k1) { PairArrayList(8) }
        val vs = list.iterate { k2s, vs -> if (k2s == k2) vs else null }
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

    inline fun replaceValues(crossinline run: (k1: KManifold, k2: KFewOnly, v: Value) -> Value): Int {
        var changed = 0
        for ((k1, k2s) in values) {
            changed += k2s.replaceBs { a, b -> run(k1, a, b) }
        }
        return changed
    }

    fun recalculateSize() {
        size = values.values.sumOf { it.size }
    }

    fun removeMajorIf(test: (k1: KManifold) -> Boolean): Boolean {
        return if (values.keys.removeIf(test)) {
            recalculateSize()
            true
        } else false
    }

    inline fun removeIf(crossinline test: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean): Int {
        if (isEmpty()) return 0
        var removed = 0
        for ((k1, k2s) in values) {
            removed += k2s.removeIf { k2, v ->
                test(k1, k2, v)
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

    fun count(test: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean): Int {
        var sum = 0
        for ((k1, k2s) in values) {
            for ((k2, v) in k2s) {
                if (test(k1, k2, v)) sum++
            }
        }
        return sum
    }

    fun countMajor(test: (k1: KManifold) -> Boolean): Int {
        var sum = 0
        for ((k1, k2s) in values) {
            if (test(k1)) sum += k2s.size
        }
        return sum
    }

    fun filterMajor(test: (k1: KManifold) -> Boolean) = values.filterKeys(test)

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