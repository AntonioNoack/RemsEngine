package me.anno.utils.search

import me.anno.maths.Maths
import me.anno.utils.structures.lists.Lists.partition1

object Median {

    // to do (low priority until sb really needs this to perform well):
    // implement median of medians
    //  - split into sqrt(len) sections,
    //  - calculate median each
    //  - calculate the median of medians -> final median
    fun <V> MutableList<V>.median(comparator: Comparator<V>): V {
        return median(0, size, comparator)
    }

    fun <V> MutableList<V>.median(i0: Int, i1: Int, comparator: Comparator<V>): V {
        return kthElement(i0, i1, size / 2, comparator)
    }

    fun <V> MutableList<V>.kthElement(k: Int, comparator: Comparator<V>): V {
        return kthElement(0, size, k, comparator)
    }

    fun <V> MutableList<V>.kthElement(i0: Int, i1: Int, k: Int, comparator: Comparator<V>): V {
        if (k !in i0 until i1) throw IllegalArgumentException()
        val rndI = i0 + (Maths.random() * (i1 - i0)).toInt()
        val rnd = this[rndI]
        val condition = { it: V ->
            comparator.compare(rnd, it) > 0
        }
        val split = partition1(i0, i1, condition)
        return when {
            split == k -> rnd
            k < split -> kthElement(0, split, k, comparator)
            else -> kthElement(split, i1, k, comparator)
        }
    }
}