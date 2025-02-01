package me.anno.utils.search

object Partition {
    @JvmStatic
    fun partition(
        start: Int, endExclusive: Int,
        compareToPivot: CompareToPivot,
        swapper: Swapper,
    ): Int {
        var i = start
        var j = (endExclusive - 1)
        if (i >= j) return i

        while (true) {
            // while front is fine, progress front
            while (i < j && compareToPivot.compareToPivot(i)) i++
            // while back is fine, progress back
            while (i < j && !compareToPivot.compareToPivot(j)) j--
            // if nothing works, swap i and j
            if (i < j) swapper.swap(i, j)
            else break
        }

        return i // i == j
    }
}