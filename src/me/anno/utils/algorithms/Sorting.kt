package me.anno.utils.algorithms

/**
 * The default sorting implementation can crash, when it detects something isn't sortable.
 * We don't want to ever crash, so let's create our own sorting, where we just assume everything is correct.
 * */
object Sorting {

    fun <V : Comparable<V>> List<V>.sorted2(): MutableList<V> {
        return ArrayList(this).apply { sort2() }
    }

    fun <V : Comparable<V>> MutableList<V>.sort2() {
        return sort2(0, size)
    }

    fun <V : Comparable<V>> MutableList<V>.sort2(from: Int, to: Int) {
        return sortWith2(from, to, Comparator.naturalOrder())
    }

    fun <V> MutableList<V>.sortWith2(comparator: Comparator<V>) {
        sortWith2(0, size, comparator)
    }

    fun <V> MutableList<V>.sortWith2(from: Int, to: Int, comparator: Comparator<V>) {
        MutableListSortable(this, comparator).sortWith2(from, to)
    }

}