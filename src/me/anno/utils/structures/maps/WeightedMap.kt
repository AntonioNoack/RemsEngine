package me.anno.utils.structures.maps

import kotlin.math.max

class WeightedMap<V>(initSize: Int = 32) : Iterable<Pair<V, Float>> {

    val content = HashMap<V, Float>(initSize)

    val size get() = content.size

    fun clear(){
        content.clear()
    }

    fun set(other: WeightedMap<V>){
        clear()
        this += other
    }

    operator fun contains(index: V) = content.containsKey(index)

    operator fun get(index: V) = content[index] ?: 0f

    override fun iterator(): Iterator<Pair<V, Float>> {
        return content.entries.map { (key, value) -> key to value }.iterator()
    }

    operator fun plusAssign(value: Pair<V, Float>){
        add(value.first, value.second)
    }

    fun add(value: V, weight: Float){
        val oldWeight = this[value]
        val newWeight = oldWeight + weight
        content[value] = newWeight
    }

    operator fun plusAssign(values: WeightedMap<V>){
        for(value in values) this += value
    }

    operator fun minusAssign(value: Pair<V, Float>){
        add(value.first, -value.second)
    }

    operator fun minusAssign(values: WeightedMap<V>){
        for(value in values) this -= value
    }

    operator fun times(multiplier: Float): WeightedMap<V> {
        val clone = WeightedMap<V>(max(32, content.size))
        for((value, weight) in content){
            clone.add(value, weight * multiplier)
        }
        return clone
    }

}