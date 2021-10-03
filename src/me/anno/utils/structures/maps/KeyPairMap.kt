package me.anno.utils.structures.maps

import me.anno.ecs.prefab.change.Path
import me.anno.utils.structures.lists.PairArrayList
import org.apache.logging.log4j.LogManager

class KeyPairMap<KManifold, KFewOnly, Value>(capacity: Int = 16) :
    Iterable<PairArrayList<KFewOnly, Value>> {

    val values = HashMap<KManifold, PairArrayList<KFewOnly, Value>>(capacity)
    var size = 0

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
        LOGGER.info("('$k1','$k2') = '$v'")
        val list = values.getOrPut(k1) { PairArrayList(8) }
        list.replaceOrAddMap(k2, v)
        size++
    }

    inline fun getOrPut(
        k1: KManifold,
        k2: KFewOnly,
        v: (k1: KManifold, k2: KFewOnly) -> Value
    ): Value {
        val list = values.getOrPut(k1) { PairArrayList(8) }
        for (pair in list) {
            if (pair.first == k2) {
                return pair.second
            }
        }
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

    inline fun removeMajorIf(noinline run: (k1: KManifold) -> Boolean): Boolean {
        return if (values.keys.removeIf(run)) {
            size = values.values.sumOf { it.size }
            true
        } else false
    }

    inline fun removeIf(crossinline run: (k1: KManifold, k2: KFewOnly, v: Value) -> Boolean): Int {
        var removed = 0
        for ((k1, k2s) in values) {
            removed += k2s.removeIf { k2, v ->
                run(k1, k2, v)
            }
        }
        size -= removed
        return removed
    }

    fun remove(k1: KManifold, k2: KFewOnly): Boolean {
        val list = values[k1] ?: return false
        return if (list.removeIf { first, _ -> first == k2 } > 0) {
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

    override fun iterator() = values.values.iterator()

    fun isEmpty() = size == 0

    companion object {
        private val LOGGER = LogManager.getLogger(KeyPairMap::class)
    }

}