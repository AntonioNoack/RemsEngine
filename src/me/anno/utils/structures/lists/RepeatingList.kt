package me.anno.utils.structures.lists

class RepeatingList<V>(override val size: Int, val content: V) : List<V> {

    class PseudoIterator<V>(val self: RepeatingList<V>, var index: Int) : ListIterator<V> {
        override fun hasNext(): Boolean = index < self.size
        override fun hasPrevious(): Boolean = index > 0
        override fun nextIndex(): Int = index
        override fun next(): V {
            index++
            return self.content
        }

        override fun previousIndex(): Int = index - 1
        override fun previous(): V {
            index--
            return self.content
        }
    }

    override fun contains(element: V): Boolean {
        return size > 0 && element == content
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        for (e in elements) if (e !in this) return false
        return true
    }

    override fun get(index: Int): V {
        return content
    }

    override fun isEmpty(): Boolean = size <= 0
    override fun iterator(): Iterator<V> = listIterator()
    override fun listIterator(): ListIterator<V> = listIterator(0)
    override fun listIterator(index: Int): ListIterator<V> {
        return PseudoIterator(this, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        return RepeatingList(toIndex - fromIndex, content)
    }

    override fun lastIndexOf(element: V): Int {
        return if (contains(element)) size - 1 else -1
    }

    override fun indexOf(element: V): Int {
        return if (contains(element)) 0 else -1
    }
}