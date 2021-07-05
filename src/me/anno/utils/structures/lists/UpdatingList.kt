package me.anno.utils.structures.lists

/**
 * quite expensive list, however it can be flexible for things, which accept just a list
 * be wary, not all things may be ok with a live changing list!
 * */
class UpdatingList<V>(val content: () -> List<V>) : List<V> {

    override val size: Int
        get() = content().size

    override fun contains(element: V): Boolean {
        return content().contains(element)
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return content().containsAll(elements)
    }

    override fun get(index: Int): V {
        return content()[index]
    }

    override fun indexOf(element: V): Int {
        return content().indexOf(element)
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun iterator(): Iterator<V> {
        return content().iterator()
    }

    override fun lastIndexOf(element: V): Int {
        return content().lastIndexOf(element)
    }

    override fun listIterator(): ListIterator<V> {
        return content().listIterator()
    }

    override fun listIterator(index: Int): ListIterator<V> {
        return content().listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        return content().subList(fromIndex, toIndex)
    }

}