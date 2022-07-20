package me.anno.utils.sorting

// Java/Kotlin don't have functions for sorting an array using a comparator on a range... why ever...
object MergeSort {

    fun <V> mergeSort(array: Array<V>, startIndex: Int, endIndex: Int, compare: Comparator<V>) {
        val copy = array.copyOfRange(startIndex, endIndex) // why is the copy needed?
        mergeSort(copy, array, startIndex, endIndex, -startIndex, compare)
    }

    private fun <V> mergeSort(in0: Array<V>, in1: Array<V>, i0: Int, i1: Int, invI0: Int, comparator: Comparator<V>) {
        var min = i0
        var max = i1
        val length = max - min
        var min0: Int
        var max0: Int
        if (length < 7) {
            // sort in O(n²)
            min0 = min
            while (min0 < max) {
                max0 = min0
                while (max0 > min && comparator.compare(in1[max0 - 1], in1[max0]) > 0) {
                    val j = max0 - 1
                    val tmp = in1[max0]
                    in1[max0] = in1[j]
                    in1[j] = tmp
                    --max0
                }
                ++min0
            }
        } else {
            min0 = min
            max0 = max
            min += invI0
            max += invI0
            val mid = min + max ushr 1
            mergeSort(in1, in0, min, mid, -invI0, comparator)
            mergeSort(in1, in0, mid, max, -invI0, comparator)
            if (comparator.compare(in0[mid - 1], in0[mid]) <= 0) {
                System.arraycopy(in0, min, in1, min0, length)
            } else {
                var dstIndex = min0
                var min1 = min
                var mid1 = mid
                while (dstIndex < max0) {
                    if (mid1 < max && (min1 >= mid || comparator.compare(in0[min1], in0[mid1]) > 0)) {
                        in1[dstIndex] = in0[mid1++]
                    } else {
                        in1[dstIndex] = in0[min1++]
                    }
                    ++dstIndex
                }
            }
        }
    }

}