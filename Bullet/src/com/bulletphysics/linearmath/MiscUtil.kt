package com.bulletphysics.linearmath

import com.bulletphysics.extras.gimpact.IntPairList
import com.bulletphysics.util.DoubleArrayList
import com.bulletphysics.util.IntArrayList
import com.bulletphysics.util.ListUtils.swapRemove
import com.bulletphysics.util.ObjectArrayList
import kotlin.math.min

/**
 * Miscellaneous utility functions.
 *
 * @author jezek2
 */
object MiscUtil {

    /**
     * Resizes list to exact size, filling with given value when expanding.
     * todo could be optimized
     */
    @JvmStatic
    fun resize(list: IntArrayList, size: Int, value: Int) {
        while (list.size() < size) {
            list.add(value)
        }

        while (list.size() > size) {
            list.remove(list.size() - 1)
        }
    }

    /**
     * Resizes list to exact size, filling with given value when expanding.
     * todo could be optimized
     */
    @JvmStatic
    fun resize(list: DoubleArrayList, size: Int, value: Double) {
        while (list.size() < size) {
            list.add(value)
        }

        while (list.size() > size) {
            list.remove(list.size() - 1)
        }
    }

    /**
     * Resizes list to exact size, filling with new instances of given class type
     * when expanding.
     */
    @JvmStatic
    fun <T> resize(list: MutableList<T>, size: Int, valueCls: Class<T>) {
        while (list.size < size) {
            list.add(valueCls.newInstance())
        }

        while (list.size > size) {
            list.swapRemove(list.lastIndex)
        }
    }

    /**
     * Resizes list to exact size, filling with new instances of given class type
     * when expanding.
     */
    @JvmStatic
    fun <T> resize(list: ObjectArrayList<T>, size: Int, valueCls: Class<T>) {
        while (list.size < size) {
            list.add(valueCls.newInstance())
        }

        while (list.size > size) {
            list.swapRemove(list.lastIndex)
        }
    }

    @JvmStatic
    fun GEN_clamped(a: Double, lb: Double, ub: Double): Double {
        return if (a < lb) lb else min(ub, a)
    }

    private fun <T> swap(list: ObjectArrayList<T>, index0: Int, index1: Int) {
        val temp = list.getQuick(index0)
        list.setQuick(index0, list.getQuick(index1))
        list.setQuick(index1, temp)
    }

    private fun swap(list: IntPairList, index0: Int, index1: Int) {
        val temp = list.getQuick(index0)
        list.setQuick(index0, list.getQuick(index1))
        list.setQuick(index1, temp)
    }

    /**
     * Sorts list using quick sort.
     *
     *
     */
    fun <T> quickSort(list: ObjectArrayList<T>, comparator: Comparator<T>) {
        // don't sort 0 or 1 elements
        if (list.size > 1) {
            quickSortInternal(list, comparator, 0, list.lastIndex)
        }
    }

    private fun <T> quickSortInternal(list: ObjectArrayList<T>, comparator: Comparator<T>, lo: Int, hi: Int) {
        // lo is the lower index, hi is the upper index
        // of the region of array a that is to be sorted
        var i = lo
        var j = hi
        val x = list.getQuick((lo + hi) shr 1)

        // partition
        do {
            while (comparator.compare(list.getQuick(i), x) < 0) i++
            while (comparator.compare(x, list.getQuick(j)) < 0) j--

            if (i <= j) {
                swap(list, i, j)
                i++
                j--
            }
        } while (i <= j)

        // recursion
        if (lo < j) {
            quickSortInternal(list, comparator, lo, j)
        }
        if (i < hi) {
            quickSortInternal(list, comparator, i, hi)
        }
    }

    /**
     * Sorts list using quick sort.
     */
    fun quickSort(list: IntPairList, comparator: LongComparator) {
        // don't sort 0 or 1 elements
        if (list.size() > 1) {
            quickSortInternal(list, comparator, 0, list.size() - 1)
        }
    }

    private fun quickSortInternal(list: IntPairList, comparator: LongComparator, lo: Int, hi: Int) {
        // lo is the lower index, hi is the upper index
        // of the region of array a that is to be sorted
        var i = lo
        var j = hi
        val x = list.getQuick((lo + hi) shr 1)

        // partition
        do {
            while (comparator.compare(list.getQuick(i), x) < 0) i++
            while (comparator.compare(x, list.getQuick(j)) < 0) j--

            if (i <= j) {
                swap(list, i, j)
                i++
                j--
            }
        } while (i <= j)

        // recursion
        if (lo < j) {
            quickSortInternal(list, comparator, lo, j)
        }
        if (i < hi) {
            quickSortInternal(list, comparator, i, hi)
        }
    }
}
