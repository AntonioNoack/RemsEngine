package me.anno.tests.cache

import me.anno.Time
import me.anno.cache.CacheSection
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class CacheRetryTest {
    @Test
    fun testCacheWithRetry() {
        val cache = CacheSection<Int, Int>("CacheRetryTest")
        val n = 10
        val limit = 3
        val done = AtomicInteger()
        val t0 = Time.nanoTime
        val sleepMillis = 100L
        for (i in 0 until n) {
            cache.getEntryLimited(i, 10_000L, limit) { key, result ->
                thread {
                    Thread.sleep(sleepMillis)
                    result.value = i
                }
            }.waitFor { value ->
                assertEquals(i, value)
                done.incrementAndGet()
            }
        }

        Sleep.waitUntil(true) {
            done.get() == n
        }

        val dtNanos = Time.nanoTime - t0
        val minDtNanos = sleepMillis * n / limit * MILLIS_TO_NANOS
        val maxDtNanos = sleepMillis * n * MILLIS_TO_NANOS
        assertTrue(
            dtNanos in minDtNanos..maxDtNanos, "Tasks took unexpected duration: " +
                    "$dtNanos !in $minDtNanos .. $maxDtNanos"
        )
    }
}