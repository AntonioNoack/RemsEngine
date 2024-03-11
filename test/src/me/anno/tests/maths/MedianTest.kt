package me.anno.tests.maths

import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.search.Median.kthElement
import me.anno.utils.search.Median.median
import me.anno.utils.structures.lists.Lists.createArrayList
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedianTest {

    /**
     * a fast median, but also just an approximation; linear time;
     * --> in my tests, this was always slower than just using the median, so I removed it
     * (I tested up to 16M elements)
     * */
    fun <V> MutableList<V>.medianOfMedians(comparator: Comparator<V>): V {
        if (size < 16) return median(comparator)
        val sqrtSize = sqrt(size.toDouble()).toInt()
        val medians = createArrayList(sqrtSize) {
            val i0 = WorkSplitter.partition(it, size, sqrtSize)
            val i1 = WorkSplitter.partition(it + 1, size, sqrtSize)
            median(i0, i1, comparator)
        }
        return medians.median(comparator)
    }

    @Test
    fun medianOfMedians() {
        val n = (1 shl 10) + 1
        val elements = createArrayList(n) { it }
        // has a chance to fail... is there any guarantees we can make?
        // -> use a static seed
        elements.shuffle(Random(1234))
        val median = elements.medianOfMedians(Int::compareTo)
        assertTrue(median in n * 5 / 11..n * 6 / 11)
    }

    @Test
    fun median() {
        val n = (1 shl 10) + 1
        val elements = createArrayList(n) { it }
        elements.shuffle()
        val median = elements.median(Int::compareTo)
        assertEquals(n / 2, median)
    }

    @Test
    fun kthElement() {
        val n = (1 shl 8) + 1
        val elements = createArrayList(n) { it }
        for (k in 0 until n) {
            elements.shuffle()
            val kth = elements.kthElement(k, Int::compareTo)
            assertEquals(k, kth)
        }
    }
}