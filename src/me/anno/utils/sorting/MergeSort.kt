package me.anno.utils.sorting

// Java/Kotlin don't have functions for sorting an array using a comparator on a range... why ever...
object MergeSort {

    private fun <V> swap(var0: Array<V>, var1: Int, var2: Int) {
        val var3 = var0[var1]
        var0[var1] = var0[var2]
        var0[var2] = var3
    }

    fun <V> mergeSort(var0: Array<V>, var1: Int, var2: Int, compare: (V, V) -> Int) {
        val var3 = var0.copyOfRange(var1, var2)
        mergeSort(var3, var0, var1, var2, -var1, compare)
    }

    private fun <V> mergeSort(in0: Array<V>, in1: Array<V>, i0: Int, i1: Int, invI0: Int, compare: (V, V) -> Int) {
        var min = i0
        var max = i1
        val length = max - min
        var min0: Int
        var max0: Int
        if (length < 7) {
            min0 = min
            while (min0 < max) {
                max0 = min0
                while (max0 > min && compare(in1[max0 - 1], in1[max0]) > 0) {
                    swap(in1, max0, max0 - 1)
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
            mergeSort(in1, in0, min, mid, -invI0, compare)
            mergeSort(in1, in0, mid, max, -invI0, compare)
            if (compare(in0[mid - 1], in0[mid]) <= 0) {
                System.arraycopy(in0, min, in1, min0, length)
            } else {
                var dstIndex = min0
                var min1 = min
                var mid1 = mid
                while (dstIndex < max0) {
                    if (mid1 < max && (min1 >= mid || compare(in0[min1], in0[mid1]) > 0)) {
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