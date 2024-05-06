package me.anno.utils.structures.maps

import me.anno.utils.structures.maps.Maps.removeIf

/**
 * map with automatic reverse map
 * */
class BiMap<K, V>(capacity: Int = 16) : HashMap<K, V>(capacity) {
    val reverse = HashMap<V, K>(capacity)

    operator fun set(key: K, value: V): V? = put(key, value)
    override fun put(key: K, value: V): V? {
        reverse[value] = key
        return super.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        reverse.putAll(from.entries.associate { it.value to it.key })
        super.putAll(from)
    }

    override fun remove(key: K): V? {
        reverse.removeIf { (_, v) -> v == key }
        return super.remove(key)
    }
}