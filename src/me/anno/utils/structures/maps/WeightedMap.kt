package me.anno.utils.structures.maps

import kotlin.math.max

/**
 * set with floating point cardinality
 * */
class WeightedMap<V>(initSize: Int = 32) : Iterable<Pair<V, Double>> {

    val content = HashMap<V, Double>(initSize)

    val size get() = content.size

    fun clear() {
        content.clear()
    }

    fun set(other: WeightedMap<V>) {
        clear()
        this += other
    }

    operator fun contains(index: V) = content.containsKey(index)

    operator fun get(index: V) = content[index] ?: 0.0

    override fun iterator(): Iterator<Pair<V, Double>> {
        return content.entries.map { (key, value) -> key to value }.iterator()
    }

    operator fun plusAssign(value: Pair<V, Double>) {
        add(value.first, value.second)
    }

    fun add(value: V, delta: Double) {
        content[value] = this[value] + delta
    }

    operator fun plusAssign(values: WeightedMap<V>) {
        for (value in values) this += value
    }

    operator fun minusAssign(value: Pair<V, Double>) {
        add(value.first, -value.second)
    }

    operator fun minusAssign(values: WeightedMap<V>) {
        for (value in values) this -= value
    }

    operator fun times(multiplier: Double): WeightedMap<V> {
        val clone = WeightedMap<V>(max(16, content.size))
        for ((value, weight) in content) {
            clone.add(value, weight * multiplier)
        }
        return clone
    }
}