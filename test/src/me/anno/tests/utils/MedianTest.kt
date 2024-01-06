package me.anno.tests.utils

import me.anno.utils.search.Median.kthElement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MedianTest {
    @Test
    fun testKthElement() {
        val n = 50
        val list = ArrayList((0 until n).toList())
        for (k in 0 until n) {
            list.shuffle()
            val kth = list.kthElement(k, Int::compareTo)
            assertEquals(k, kth)
        }
    }
}