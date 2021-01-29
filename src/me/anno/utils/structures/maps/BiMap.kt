package me.anno.utils.structures.maps

class BiMap<K, V>(capacity: Int): HashMap<K, V>(capacity){
    val reverse = HashMap<V, K>(capacity)
    override fun put(key: K, value: V): V? {
        reverse[value] = key
        return super.put(key, value)
    }
    override fun putAll(from: Map<out K, V>) {
        reverse.putAll(from.entries.associate { it.value to it.key })
        super.putAll(from)
    }
}