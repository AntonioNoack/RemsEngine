package me.anno.utils.search

import me.anno.maths.Maths
import me.anno.maths.Maths.log2i
import me.anno.maths.bvh.SplitMethod.Companion.mid
import me.anno.utils.assertions.assertContains
import me.anno.utils.structures.lists.Lists.swap
import kotlin.random.Random

/**
 * This class finds the k-th element, or the median, in an unordered collection.
 *
 * After calling these functions, the collection will be slightly better sorted,
 * and partitioned by the returned median.
 * */
object Median {

    @JvmStatic
    fun <V> MutableList<V>.median(comparator: Comparator<V>): V {
        return median(0, size, comparator)
    }

    @JvmStatic
    fun <V> MutableList<V>.median(start: Int, endExclusive: Int, comparator: Comparator<V>): V {
        return kthElement(start, endExclusive, mid(start, endExclusive), comparator)
    }

    @JvmStatic
    fun <V> MutableList<V>.kthElement(k: Int, comparator: Comparator<V>): V {
        return kthElement(0, size, k, comparator)
    }

    @JvmStatic
    fun <V> MutableList<V>.kthElement(start: Int, endExclusive: Int, k: Int, comparator: Comparator<V>): V {
        return kthElement(start, endExclusive, k, { i, j -> swap(i, j) }, this::get, comparator)
    }

    @JvmStatic
    fun <V> median(
        start: Int, endExclusive: Int, swapper: Swapper, getElement: GetElement<V>, comparator: Comparator<V>
    ): V = kthElement(start, endExclusive, mid(start, endExclusive), swapper, getElement, comparator)

    @JvmStatic
    fun <V> kthElement(
        start: Int, endExclusive: Int, k: Int,
        swapper: Swapper, getElement: GetElement<V>, comparator: Comparator<V>,
        random: Random = Maths.getRandom() // if you need determinism, you can set this
    ): V {
        assertContains(k, start until endExclusive, "k !in i0 until i1")

        var startI = start
        var endI = endExclusive

        var rnd: V? = null

        val compareToPivot = CompareToPivot { idx ->
            comparator.compare(rnd, getElement[idx]) > 0
        }

        while (true) {
            rnd = getElement[random.nextInt(startI, endI)]
            val split = Partition.partition(startI, endI, compareToPivot, swapper)
            when {
                split == k -> return rnd
                k < split -> endI = split
                else -> startI = split
            }
        }
    }

    @JvmStatic
    fun medianApprox(
        start: Int, endExclusive: Int, pivot0: Double,
        sampler: Sampler, swapper: Swapper, random: Random,
    ): Int {
        var pivot = 0.0
        val compareToPivot = CompareToPivot { j ->
            sampler.sample(j) >= pivot
        }

        val count = endExclusive - start
        val count4th = count.ushr(2)
        val minAcceptableI = start + count4th
        val maxAcceptableI = start + count4th * 3
        val maxNumTries = count.toFloat().log2i().shr(1)
        for (tryIndex in 0 until maxNumTries) {

            // using this guessed pivot as a first try reduced the time from 80ms to 57ms on my Ryzen 9 7950x3d
            @Suppress("AssignedValueIsNeverRead") // pivot is read by compareToPivot
            if (tryIndex > 0) {
                // try finding a better median
                pivot = 0.0
                repeat(5) {
                    val idx = random.nextInt(start, endExclusive)
                    pivot += sampler.sample(idx)
                }
                pivot *= 0.2
            } else pivot = pivot0

            val splitI = Partition.partition(start, endExclusive, compareToPivot, swapper)
            if (splitI in minAcceptableI..maxAcceptableI) { // >50% chance
                return splitI
            }
        }
        return mid(start, endExclusive)
    }
}