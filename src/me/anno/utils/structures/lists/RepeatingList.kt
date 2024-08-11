package me.anno.utils.structures.lists

class RepeatingList<V>(override val size: Int, val content: V) : SimpleList<V>() {

    override fun get(index: Int): V = content

    override fun contains(element: V): Boolean {
        return size > 0 && element == content
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all { it == content }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        return RepeatingList(toIndex - fromIndex, content)
    }

    override fun lastIndexOf(element: V): Int = if (contains(element)) size - 1 else -1
    override fun indexOf(element: V): Int = if (contains(element)) 0 else -1
}