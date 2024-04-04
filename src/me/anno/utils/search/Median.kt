package me.anno.utils.search

import me.anno.maths.Maths
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.partition1
import kotlin.math.sqrt

/**
 * This class finds the k-th element, or the median,
 * in an unordered collection.
 *
 * After calling these functions, the collection will be slightly better sorted.
 * */
object Median {

    fun <V> MutableList<V>.median(comparator: Comparator<V>): V {
        return median(0, size, comparator)
    }

    fun <V> MutableList<V>.median(i0: Int, i1: Int, comparator: Comparator<V>): V {
        return kthElement(i0, i1, (i0 + i1) / 2, comparator)
    }

    fun <V> MutableList<V>.kthElement(k: Int, comparator: Comparator<V>): V {
        return kthElement(0, size, k, comparator)
    }

    fun <V> MutableList<V>.kthElement(i0: Int, i1: Int, k: Int, comparator: Comparator<V>): V {
        if (k !in i0 until i1) throw IndexOutOfBoundsException("k !in i0 until i1")
        val rndI = i0 + (Maths.random() * (i1 - i0)).toInt()
        val rnd = this[rndI]
        val condition = { it: V ->
            comparator.compare(rnd, it) > 0
        }
        val split = partition1(i0, i1, condition)
        return when {
            split == k -> rnd
            k < split -> kthElement(i0, split, k, comparator)
            else -> kthElement(split, i1, k, comparator)
        }
    }
}