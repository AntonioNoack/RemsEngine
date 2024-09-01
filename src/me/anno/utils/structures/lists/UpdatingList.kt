package me.anno.utils.structures.lists

import me.anno.utils.structures.CachedValue

class UpdatingList<V>(timeoutMillis: Long, getter: () -> List<V>) : SimpleList<V>() {
    constructor(getter: () -> List<V>) : this(500, getter)

    private val values by CachedValue(timeoutMillis, getter)
    override val size: Int get() = values.size
    override fun get(index: Int): V = values[index]

    // these are not strictly necessary, but they're good to have for consistency
    override fun contains(element: V): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<V>): Boolean = values.containsAll(elements)
    override fun indexOf(element: V): Int = values.indexOf(element)
    override fun lastIndexOf(element: V): Int = values.lastIndexOf(element)
    override fun subList(fromIndex: Int, toIndex: Int): List<V> = values.subList(fromIndex, toIndex)
    override fun listIterator(index: Int): ListIterator<V> = values.listIterator(index)
    override fun toString(): String = values.toString()
}