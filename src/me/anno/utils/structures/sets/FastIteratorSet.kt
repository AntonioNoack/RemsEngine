package me.anno.utils.structures.sets

import me.anno.utils.assertions.assertEquals

/**
 * Much faster than HashSet for iterating over thousands of instances;
 * probably somewhat slower when inserting;
 *
 * not thread-safe!
 * */
class FastIteratorSet<V>(initialCapacity: Int = 16) : MutableCollection<V> {

    private val idLookup = HashMap<V, Int>(initialCapacity)
    private val values = ArrayList<V>(initialCapacity)

    override val size: Int get() = values.size

    fun setContains(instance: V, shallContain: Boolean) {
        if (!shallContain) remove(instance)
        else add(instance)
    }

    override fun add(element: V): Boolean {
        val prevId = idLookup.putIfAbsent(element, values.size)
        return if (prevId == null) {
            values.add(element)
            true
        } else false
    }

    override fun remove(element: V): Boolean {
        val id = idLookup.remove(element)
            ?: return false
        swapRemove(id)
        return true
    }

    fun removeAt(id: Int): V {
        val value = values[id]
        assertEquals(id, idLookup.remove(value))
        swapRemove(id)
        return value
    }

    private fun swapRemove(id: Int) {
        if (id < values.lastIndex) {
            val moved = values.last()
            idLookup[moved] = id
            values[id] = moved
        }
        values.removeLast()
    }

    fun toggleContains(instance: V) {
        if (!remove(instance)) {
            add(instance)
        }
    }

    override fun clear() {
        idLookup.clear()
        values.clear()
    }

    fun asList(): List<V> = values
    override fun contains(element: V): Boolean = element in idLookup
    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all { element -> element in idLookup }
    }

    override fun iterator(): MutableIterator<V> = values.iterator()
    override fun isEmpty(): Boolean = values.isEmpty()
    fun isNotEmpty(): Boolean = values.isNotEmpty()
    fun first(): V = values.first()
    fun firstOrNull(): V? = values.firstOrNull()

    override fun addAll(elements: Collection<V>): Boolean {
        var changed = false
        for (element in elements) {
            if (add(element)) changed = true
        }
        return changed
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        var changed = false
        for (element in elements) {
            if (remove(element)) changed = true
        }
        return changed
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        var changed = false
        for (i in values.lastIndex downTo 0) {
            val value = values[i]
            if (value !in elements) {
                removeAt(i)
                changed = true
            }
        }
        return changed
    }
}