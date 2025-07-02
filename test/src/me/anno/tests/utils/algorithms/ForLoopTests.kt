package me.anno.tests.utils.algorithms

import me.anno.utils.algorithms.ForLoop
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForLoopTests {

    @Test
    fun testForLoopBasicRange() {
        val result = ArrayList<Int>()
        ForLoop.forLoop(0, 5, 1) {
            result.add(it)
        }
        assertEquals(listOf(0, 1, 2, 3, 4), result)
    }

    @Test
    fun testForLoopWithStep() {
        val result = ArrayList<Int>()
        ForLoop.forLoop(0, 10, 2) {
            result.add(it)
        }
        assertEquals(listOf(0, 2, 4, 6, 8), result)
    }

    @Test
    fun testForLoopEmptyRange() {
        val result = ArrayList<Int>()
        ForLoop.forLoop(5, 5, 1) {
            result.add(it)
        }
        assertTrue(result.isEmpty())
    }

    @Test
    fun testForLoopStartGreaterThanEnd() {
        val result = ArrayList<Int>()
        ForLoop.forLoop(10, 5, 1) {
            result.add(it)
        }
        assertTrue(result.isEmpty())
    }

    @Test
    fun testForLoopWithLargeStep() {
        val result = ArrayList<Int>()
        ForLoop.forLoop(0, 10, 5) {
            result.add(it)
        }
        assertEquals(listOf(0, 5), result)
    }

    @Test
    fun testForLoopSafelyExactFit() {
        val result = ArrayList<Int>()
        ForLoop.forLoopSafely(10, 5) {
            result.add(it)
        }
        assertEquals(listOf(0, 5), result)
    }

    @Test
    fun testForLoopSafelyStepTooLarge() {
        val result = ArrayList<Int>()
        ForLoop.forLoopSafely(4, 5) {
            result.add(it)
        }
        assertTrue(result.isEmpty())
    }

    @Test
    fun testForLoopSafelyWithStepOne() {
        val result = ArrayList<Int>()
        ForLoop.forLoopSafely(3, 1) {
            result.add(it)
        }
        assertEquals(listOf(0, 1, 2), result)
    }
}