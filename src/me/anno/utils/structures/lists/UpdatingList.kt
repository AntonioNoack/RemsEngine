package me.anno.utils.structures.lists

import me.anno.utils.structures.CachedValue

class UpdatingList<V>(timeoutMillis: Long, getter: () -> List<V>) : List<V> {
    private val values by CachedValue(timeoutMillis, getter)
    override val size: Int get() = values.size
    override fun get(index: Int): V = values[index]
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun iterator(): Iterator<V> = values.iterator()
    override fun listIterator(): ListIterator<V> = values.listIterator()
    override fun listIterator(index: Int): ListIterator<V> = values.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<V> = values.subList(fromIndex, toIndex)
    override fun lastIndexOf(element: V): Int = values.lastIndexOf(element)
    override fun indexOf(element: V): Int = values.indexOf(element)
    override fun containsAll(elements: Collection<V>): Boolean = values.containsAll(elements)
    override fun contains(element: V): Boolean = values.contains(element)
}