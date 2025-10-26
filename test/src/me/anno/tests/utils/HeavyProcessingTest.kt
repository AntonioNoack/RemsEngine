package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.hpc.HeavyProcessing.splitWork
import org.joml.Vector2i
import org.junit.jupiter.api.Test

class HeavyProcessingTest {
    @Test
    fun testSplitWork() {
        assertEquals(Vector2i(5, 1), splitWork(50, 10, 5))
        assertEquals(Vector2i(7, 1), splitWork(50, 10, 7))
        assertEquals(Vector2i(4, 2), splitWork(50, 10, 8))
    }
}