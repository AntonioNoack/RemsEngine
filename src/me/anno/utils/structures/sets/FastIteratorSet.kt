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

    fun getList(): List<V> = values
}