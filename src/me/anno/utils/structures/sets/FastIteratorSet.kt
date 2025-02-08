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
        if (!shallContain) remove(instance)
        else add(instance)
    }

    private fun add(instance: V): Boolean {
        val prevId = idLookup.putIfAbsent(instance, values.size)
        return if (prevId == null) {
            values.add(instance)
            true
        } else false
    }

    private fun remove(instance: V): Boolean {
        val id = idLookup.remove(instance) ?: return false
        if (id < values.lastIndex) {
            val moved = values.last()
            idLookup[moved] = id
            values[id] = moved
        }
        values.removeLast()
        return true
    }

    fun toggleContains(instance: V) {
        if (!remove(instance)) {
            add(instance)
        }
    }

    fun clear() {
        idLookup.clear()
        values.clear()
    }

    fun asList(): List<V> = values
    fun contains(instance: V): Boolean = instance in idLookup
    fun isEmpty(): Boolean = values.isEmpty()
    fun isNotEmpty(): Boolean = values.isNotEmpty()
    fun first(): V = values.first()
    fun firstOrNull(): V? = values.firstOrNull()
}