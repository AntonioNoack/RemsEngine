package me.anno.utils.algorithms

import me.anno.utils.structures.lists.Lists.swap

class MutableListSortable<V>(val list: MutableList<V>, val comparator: Comparator<V>) : Sortable {

    private var stored: V? = null
    override fun store(srcI: Int) {
        stored = list[srcI]
    }

    override fun restore(dstI: Int) {
        @Suppress("UNCHECKED_CAST")
        list[dstI] = stored as V
    }

    override fun move(dstI: Int, srcI: Int) {
        list[dstI] = list[srcI]
    }

    override fun compare(indexI: Int, indexJ: Int): Int {
        return comparator.compare(list[indexI], list[indexJ])
    }

    override fun swap(indexI: Int, indexJ: Int) {
        list.swap(indexI, indexJ)
    }
}