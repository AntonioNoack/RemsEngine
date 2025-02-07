package me.anno.utils.structures.sets

/**
 * Much faster than HashSet for iterating over thousands of instances;
 * probably somewhat slower when inserting;
 *
 * not thread-safe!
 * */
class FastIteratorSet<V>(initialCapacity: Int = 16) {

    private val idLookup = HashMap<V, Int>(initialCapacity)
    private val values = ArrayList<V>(initialCapacity)

    fun setContains(instance: V, shallContain: Boolean) {
        if (shallContain) {
            val id = idLookup[instance] ?: return
            values[id] = values.last()
            values.removeLast()
        } else {
            idLookup[instance] = values.size
            values.add(instance)
        }
    }

    fun toggleContains(instance: V) {
        val id = idLookup[instance]
        if (id != null) {
            values[id] = values.last()
            values.removeLast()
        } else {
            idLookup[instance] = values.size
            values.add(instance)
        }
    }

    fun asList(): List<V> = values
    fun contains(instance: V): Boolean = instance in idLookup
    fun isEmpty(): Boolean = values.isEmpty()
    fun isNotEmpty(): Boolean = values.isNotEmpty()
    fun first(): V = values.first()
    fun firstOrNull(): V? = values.firstOrNull()
}