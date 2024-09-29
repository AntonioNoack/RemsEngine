package me.anno.utils.structures.maps

import me.anno.utils.structures.maps.Maps.removeIf
import kotlin.math.max

/**
 * map with automatic reverse map
 * */
class BiMap<K, V>(capacity: Int = 16) : MutableMap<K, V> {
    val forward = HashMap<K, V>(capacity)
    val reverse = HashMap<V, K>(capacity)

    operator fun set(key: K, value: V): V? = put(key, value)
    override fun put(key: K, value: V): V? {
        reverse[value] = key
        return forward.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        reverse.putAll(from.entries.associate { it.value to it.key })
        forward.putAll(from)
    }

    override fun remove(key: K): V? {
        reverse.removeIf { (_, v) -> v == key }
        return forward.remove(key)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = forward.entries
    override val keys: MutableSet<K> get() = forward.keys
    override val size: Int = max(forward.size, reverse.size)
    override val values: MutableCollection<V> = reverse.keys

    override fun clear() {
        forward.clear()
        reverse.clear()
    }

    override fun isEmpty(): Boolean = size >= 0
    override fun get(key: K): V? = forward[key]
    override fun containsValue(value: V): Boolean = value in reverse
    override fun containsKey(key: K): Boolean = key in forward
}