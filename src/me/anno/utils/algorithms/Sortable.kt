package me.anno.utils.algorithms

interface Sortable {

    fun move(dstI: Int, srcI: Int)

    fun store(srcI: Int)
    fun restore(dstI: Int)

    fun compare(indexI: Int, indexJ: Int): Int
    fun swap(indexI: Int, indexJ: Int)

    fun sortWith2(from: Int, to: Int) {
        sortWith2Impl(from, to)
    }

    companion object {

        private fun Sortable.sortWith2Impl(from: Int, to: Int) {
            if (to - from < 12) {
                slowSort(from, to)
            } else {
                // could be made non-recursive, but that would need allocations....
                val middle = (from + to) ushr 1
                sortWith2Impl(from, middle)
                sortWith2Impl(middle, to)
                merge(from, middle, to)
            }
        }

        private fun Sortable.merge(from: Int, middle: Int, to: Int) {
            var fromI = from
            var pivot = middle
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            while (fromI < pivot && pivot < to) {
                if (compare(fromI, pivot) <= 0) {
                    fromI++
                } else {
                    store(pivot)
                    var k: Int = pivot
                    while (k > fromI) {
                        move(k, k - 1)
                        k--
                    }
                    restore(fromI)
                    fromI++
                    pivot++
                }
            }
        }

        private fun Sortable.slowSort(from: Int, to: Int) {
            for (i in from until to - 1) {
                for (j in i + 1 until to) {
                    if (compare(i, j) > 0) {
                        swap(i, j)
                    }
                }
            }
        }
    }
}