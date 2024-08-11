package me.anno.utils.structures.lists

class SimpleListIterator<V>(val list: List<V>, var nextIndex: Int) : ListIterator<V> {
    override fun hasNext(): Boolean = nextIndex < list.size
    override fun hasPrevious(): Boolean = nextIndex > 0
    override fun next(): V = list[nextIndex++]
    override fun nextIndex(): Int = nextIndex
    override fun previous(): V = list[--nextIndex]
    override fun previousIndex(): Int = nextIndex - 1
}