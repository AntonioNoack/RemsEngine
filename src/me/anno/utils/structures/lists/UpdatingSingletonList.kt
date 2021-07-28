package me.anno.utils.structures.lists

class UpdatingSingletonList<V>(val content: () -> V) : List<V> {

    override val size: Int = 1
    override fun contains(element: V): Boolean {
        return element == content()
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return elements.all { contains(it) }
    }

    override fun get(index: Int): V {
        return content()
    }

    override fun indexOf(element: V): Int {
        return if (element == content()) 0 else -1
    }

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<V> {
        return listIterator()
    }

    override fun lastIndexOf(element: V): Int {
        return indexOf(element)
    }

    override fun listIterator(): ListIterator<V> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<V> {
        return object : ListIterator<V> {
            var done = false
            override fun hasNext(): Boolean = !done
            override fun hasPrevious(): Boolean = done
            override fun next(): V {
                done = true
                return content()
            }

            override fun nextIndex(): Int = if (done) 1 else 0
            override fun previous(): V {
                done = false
                return content()
            }

            override fun previousIndex(): Int = nextIndex() - 1
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        return if (fromIndex > 0) emptyList()
        else this
    }


}