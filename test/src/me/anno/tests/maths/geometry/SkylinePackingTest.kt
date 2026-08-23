package me.anno.tests.maths.geometry

import me.anno.maths.geometry.SkylinePacking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SkylinePackingTest {

    private fun unpackX(value: Long): Int {
        return (value shr 32).toInt()
    }

    private fun unpackY(value: Long): Int {
        return value.toInt()
    }

    @Test
    fun emptyAtlas() {
        val packing = SkylinePacking(100, 100)
        val result = packing.allocate(20, 30)

        assertNotEquals(-1L, result)
        assertEquals(0, unpackX(result))
        assertEquals(0, unpackY(result))
    }

    @Test
    fun exactAtlasSize() {
        val packing = SkylinePacking(100, 100)
        val result = packing.allocate(100, 100)

        assertNotEquals(-1L, result)
        assertEquals(0, unpackX(result))
        assertEquals(0, unpackY(result))

        assertEquals(-1L, packing.allocate(1, 1))
    }

    @Test
    fun rectangleTooLarge() {
        val packing = SkylinePacking(100, 100)

        assertEquals(-1L, packing.allocate(101, 10))
        assertEquals(-1L, packing.allocate(10, 101))
        assertEquals(-1L, packing.allocate(101, 101))
    }

    @Test
    fun rectangleTooWideForRemainingSpace() {
        val packing = SkylinePacking(100, 100)
        val a = packing.allocate(60, 20)

        assertNotEquals(-1L, a)

        val b = packing.allocate(41, 20)
        assertNotEquals(-1L, b)

        assertEquals(0, unpackX(b))
        assertEquals(20, unpackY(b))
    }

    @Test
    fun secondRectangleUsesRemainingSpace() {
        val packing = SkylinePacking(100, 100)

        val a = packing.allocate(40, 20)
        val b = packing.allocate(30, 10)

        assertEquals(0, unpackX(a))
        assertEquals(0, unpackY(a))

        assertEquals(40, unpackX(b))
        assertEquals(0, unpackY(b))
    }

    @Test
    fun rectangleCanSpanMultipleSkylineNodes() {
        val packing = SkylinePacking(100, 100)

        packing.allocate(20, 20)
        packing.allocate(20, 40)

        val result = packing.allocate(30, 10)

        assertNotEquals(-1L, result)

        // It must be placed above the 40-pixel-high region
        // if it spans that region.
        assertEquals(40, unpackY(result))
    }

    @Test
    fun splitNodeCorrectly() {
        val packing = SkylinePacking(100, 100)

        packing.allocate(100, 10)

        val result = packing.allocate(20, 20)

        assertNotEquals(-1L, result)
        assertEquals(0, unpackX(result))
        assertEquals(10, unpackY(result))
    }

    @Test
    fun multipleSplits() {
        val packing = SkylinePacking(100, 100)

        packing.allocate(30, 10)
        packing.allocate(30, 20)
        packing.allocate(30, 30)

        val result = packing.allocate(10, 5)

        assertNotEquals(-1L, result)
        assertEquals(90, unpackX(result))
        assertEquals(0, unpackY(result))
    }

    @Test
    fun exactHeightMerge() {
        val packing = SkylinePacking(
            width = 100,
            height = 100,
            heightTolerance = 0
        )

        packing.allocate(30, 20)
        packing.allocate(30, 20)

        // The skyline should have merged the two 20-high
        // segments into one continuous segment.
        val result = packing.allocate(40, 1)

        assertNotEquals(-1L, result)
        assertEquals(60, unpackX(result))
        assertEquals(0, unpackY(result))
    }

    @Test
    fun similarHeightMerge() {
        val packing = SkylinePacking(
            width = 100,
            height = 100,
            heightTolerance = 2
        )

        packing.allocate(30, 20)
        packing.allocate(30, 22)

        // With tolerance=2, the skyline heights 20 and 22
        // should be merged conservatively to height 22.
        val result = packing.allocate(40, 1)

        assertNotEquals(-1L, result)

        // It cannot fit at y=0 because the merged skyline
        // occupies the entire first 60 pixels up to y=22.
        assertEquals(0, unpackX(result))
        assertEquals(22, unpackY(result))
    }

    @Test
    fun heightDifferenceOutsideToleranceDoesNotMerge() {
        val packing = SkylinePacking(
            width = 100,
            height = 100,
            heightTolerance = 2
        )

        packing.allocate(30, 20)
        packing.allocate(30, 23)

        val result = packing.allocate(1, 1)

        assertNotEquals(-1L, result)

        // The 1x1 rectangle should still be able to use the
        // lower 20-high part.
        assertEquals(60, unpackX(result))
        assertEquals(0, unpackY(result))
    }

    @Test
    fun toleranceIsConservative() {
        val packing = SkylinePacking(
            width = 100,
            height = 100,
            heightTolerance = 100
        )

        packing.allocate(30, 10)
        packing.allocate(30, 20)

        // Everything is merged to height 20. This wastes space,
        // but must not cause overlap.
        val result = packing.allocate(40, 1)

        assertEquals(20, unpackY(result))
    }

    @Test
    fun allocationsStayInsideAtlas() {
        val width = 128
        val height = 128

        val packing = SkylinePacking(
            width,
            height,
            heightTolerance = 2
        )

        val random = Random(12345)

        repeat(1000) {
            val w = random.nextInt(1, 32)
            val h = random.nextInt(1, 32)

            val result = packing.allocate(w, h)

            if (result != -1L) {
                val x = unpackX(result)
                val y = unpackY(result)

                assertTrue(x >= 0)
                assertTrue(y >= 0)
                assertTrue(x + w <= width)
                assertTrue(y + h <= height)
            }
        }
    }

    @Test
    fun allocationsNeverOverlap() {
        data class Rect(
            val x: Int,
            val y: Int,
            val w: Int,
            val h: Int
        )

        val width = 256
        val height = 256

        val packing = SkylinePacking(
            width,
            height,
            heightTolerance = 2
        )

        val random = Random(123456)

        val rectangles = ArrayList<Rect>()

        repeat(5000) {
            val w = random.nextInt(1, 64)
            val h = random.nextInt(1, 64)

            val result = packing.allocate(w, h)

            if (result == -1L) return@repeat

            val rect = Rect(
                unpackX(result),
                unpackY(result),
                w,
                h
            )

            for (other in rectangles) {
                val overlapX =
                    rect.x < other.x + other.w &&
                    rect.x + rect.w > other.x

                val overlapY =
                    rect.y < other.y + other.h &&
                    rect.y + rect.h > other.y

                assertFalse(
                    overlapX && overlapY,
                    "Rectangles overlap: $rect and $other"
                )
            }

            rectangles += rect
        }
    }

    @Test
    fun randomizedStressTest() {
        val random = Random(987654321)

        repeat(100) { iteration ->
            val width = random.nextInt(16, 256)
            val height = random.nextInt(16, 256)

            val tolerance = random.nextInt(0, 4)

            val packing = SkylinePacking(
                width,
                height,
                tolerance
            )

            val rectangles = ArrayList<IntArray>()

            repeat(1000) {
                val w = random.nextInt(1, minOf(width, 64) + 1)
                val h = random.nextInt(1, minOf(height, 64) + 1)

                val result = packing.allocate(w, h)

                if (result == -1L) {
                    return@repeat
                }

                val x = unpackX(result)
                val y = unpackY(result)

                assertTrue(
                    x >= 0 && y >= 0,
                    "Negative position"
                )

                assertTrue(
                    x + w <= width,
                    "Rectangle exceeds width"
                )

                assertTrue(
                    y + h <= height,
                    "Rectangle exceeds height"
                )

                val current = intArrayOf(x, y, w, h)

                for (other in rectangles) {
                    val overlap =
                        x < other[0] + other[2] &&
                        x + w > other[0] &&
                        y < other[1] + other[3] &&
                        y + h > other[1]

                    assertFalse(
                        overlap,
                        "Iteration $iteration: " +
                            "overlap between " +
                            current.contentToString() +
                            " and " +
                            other.contentToString()
                    )
                }

                rectangles += current
            }
        }
    }
}