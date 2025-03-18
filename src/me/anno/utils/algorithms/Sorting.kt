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
        if (to - from < 12) {
            slowSort(from, to, comparator)
        } else {
            // could be made non-recursive, but that would need allocations....
            val middle = (from + to) ushr 1
            sortWith2(from, middle, comparator)
            sortWith2(middle, to, comparator)
            merge(from, middle, to, comparator)
        }
    }

    private fun <V> MutableList<V>.merge(from: Int, middle: Int, to: Int, comparator: Comparator<V>) {
        var fromI = from
        var pivot = middle
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        while (fromI < pivot && pivot < to) {
            if (comparator.compare(get(fromI), get(pivot)) <= 0) {
                fromI++
            } else {
                val tmp: V = get(pivot)
                var k: Int = pivot
                while (k > fromI) {
                    set(k, get(k - 1))
                    k--
                }
                set(fromI, tmp)
                fromI++
                pivot++
            }
        }
    }

    private fun <V> MutableList<V>.slowSort(from: Int, to: Int, comparator: Comparator<V>) {
        for (i in from until to - 1) {
            for (j in i + 1 until to) {
                val vi = get(i)
                val vj = get(j)
                if (comparator.compare(vi, vj) > 0) {
                    set(i, vj)
                    set(j, vi)
                }
            }
        }
    }
}