package me.anno.tests.utils

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.abs
import kotlin.math.min

class TimeTest {

    val delayMs = 16L

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testProgress() {
        testTimeSpeed(1.0)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testProgressFast() {
        testTimeSpeed(5.0)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testProgressSlow() {
        testTimeSpeed(0.2)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testProgressStopped() {
        testTimeSpeed(0.0)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testProgressBackwards() {
        testTimeSpeed(0.0)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testDtClamping() {
        testTimeSpeed(1.0, 200)
    }

    fun ensureClassesAreLoaded() {
        Time.timeSpeed = 0.0
        Time.updateTime()
    }

    class TimeRecord {
        val gameTime = Time.gameTime
        val gameTimeN = Time.gameTimeN
        val lastGameTime = Time.lastGameTime
        val lastGameTimeN = Time.lastGameTimeN
        val nanoTime = Time.nanoTime
        val deltaTime = Time.deltaTime
        val uiDeltaTime = Time.uiDeltaTime
        val rawDeltaTime = Time.rawDeltaTime
        val frameIndex = Time.frameIndex
        val currentFPS = Time.currentFPS
        val frameTimeNanos = Time.frameTimeNanos
        val speed = Time.timeSpeed
    }

    fun record(): TimeRecord {
        Time.updateTime()
        return TimeRecord()
    }

    fun testTimeSpeed(timeSpeed: Double, delayMs: Long = this.delayMs) {
        ensureClassesAreLoaded()
        Time.timeSpeed = timeSpeed

        val t0 = System.nanoTime()
        val start = record()
        val t1 = System.nanoTime()

        Thread.sleep(delayMs)

        val t3 = System.nanoTime()
        val end = record()
        val t4 = System.nanoTime()

        val extraTolerance = (t1 - t0) + (t4 - t3) // just 0.1 ms
        println("Extra tolerance: $extraTolerance ns")

        val delayNanos = t3 - t0
        println("Wanted to sleep for $delayMs, actually slept ${delayNanos / MILLIS_TO_NANOS}")

        val tStart = Time.startTimeN
        assertEquals(timeSpeed, Time.timeSpeed)
        assertEquals(min(timeSpeed * delayNanos * 1e-9, 0.1), end.gameTime - start.gameTime, 0.001)
        assertEquals(
            min(timeSpeed * delayNanos, 100.0 * MILLIS_TO_NANOS),
            (end.gameTimeN - start.gameTimeN).toDouble(), 1e6
        )

        // check gameTime of start being equals to lastGameTime of end
        assertEquals(start.gameTime, end.lastGameTime)
        assertEquals(start.gameTimeN, end.lastGameTimeN)

        // check nanoTime being later than frameTimeNanos
        assertTrue(end.nanoTime >= end.frameTimeNanos)
        assertTrue(start.nanoTime >= start.frameTimeNanos)

        // check (start/end).nanoTime being roughly the same as what we recorded
        assertEqualsN(t3, end.nanoTime + tStart, extraTolerance)
        assertEqualsN(t0, start.nanoTime + tStart, extraTolerance)

        assertEquals(min(delayNanos * timeSpeed * 1e-9, 0.1), end.deltaTime, 0.001)
        assertEquals(min(delayNanos / 1e9, 0.1), end.uiDeltaTime, 0.001)
        assertEquals(delayNanos / 1e9, end.rawDeltaTime, 0.001)
        assertEquals(1, end.frameIndex - start.frameIndex)
        // assertEquals(1e9 / delayNanos, end.currentFPS, 0.1)

        // check that frameTimeNanos is just our time source plus offset
        assertEqualsN(t0, start.frameTimeNanos + tStart, extraTolerance)
        assertEqualsN(t3, end.frameTimeNanos + tStart, extraTolerance)
        // println("$t0 + ${start.frameTimeNanos} + $tStart")
    }

    fun assertEqualsN(expected: Long, actual: Long, absoluteTolerance: Long) {
        assertTrue(
            abs(expected - actual) <= absoluteTolerance,
            "|$expected - $actual| = ${abs(expected - actual)} > $absoluteTolerance"
        )
    }
}