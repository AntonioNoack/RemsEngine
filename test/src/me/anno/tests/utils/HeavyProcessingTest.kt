package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.hpc.HeavyProcessing.splitWork
import me.anno.utils.structures.tuples.IntPair
import org.junit.jupiter.api.Test

class HeavyProcessingTest {
    @Test
    fun testSplitWork() {
        assertEquals(IntPair(5, 1), splitWork(50, 10, 5))
        assertEquals(IntPair(7, 1), splitWork(50, 10, 7))
        assertEquals(IntPair(4, 2), splitWork(50, 10, 8))
    }
}