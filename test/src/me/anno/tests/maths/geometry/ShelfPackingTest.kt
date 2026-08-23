package me.anno.tests.maths.geometry

import me.anno.maths.Packing.pack64
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.maths.geometry.ShelfPacking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ShelfPackingTest {

    private fun unpackX(a: Long) = unpackHighFrom64(a)
    private fun unpackY(a: Long) = unpackLowFrom64(a)

    private fun assertEquals(a: Long, b: Long) {
        assertEquals(a, b) {
            "(${unpackX(a)},${unpackY(a)}) != (${unpackX(b)},${unpackY(b)})"
        }
    }

    @Test
    fun testInvalidRectangle() {
        val packing = ShelfPacking(100, 100)

        assertEquals(0L, packing.allocate(0, 10))
        assertEquals(0L, packing.allocate(10, 0))
        assertEquals(0L, packing.allocate(-1, 10))
        assertEquals(0L, packing.allocate(10, -1))
    }

    @Test
    fun testRectangleTooLarge() {
        val packing = ShelfPacking(100, 100)

        assertEquals(-1L, packing.allocate(101, 10))
        assertEquals(-1L, packing.allocate(10, 101))
    }

    @Test
    fun testExactFit() {
        val packing = ShelfPacking(100, 100)
        assertEquals(pack64(0, 0), packing.allocate(100, 100))
        assertEquals(-1L, packing.allocate(1, 1))
    }

    @Test
    fun testRectanglesShareShelf() {
        val packing = ShelfPacking(100, 100)
        assertEquals(pack64(0, 0), packing.allocate(20, 10))
        assertEquals(pack64(20, 0), packing.allocate(30, 10))
        assertEquals(pack64(50, 0), packing.allocate(50, 10))
    }

    @Test
    fun testShelfStartsWhenWidthIsExhausted() {
        val packing = ShelfPacking(100, 100)
        assertEquals(pack64(0, 0), packing.allocate(60, 10))
        assertEquals(pack64(0, 10), packing.allocate(50, 10))
    }

    @Test
    fun testShelfHeight() {
        val packing = ShelfPacking(100, 100)

        assertEquals(pack64(0, 0), packing.allocate(20, 30))
        assertEquals(pack64(20, 0), packing.allocate(20, 30))

        // Doesn't fit the first shelf vertically, so starts a new shelf.
        assertEquals(pack64(0, 30), packing.allocate(20, 40))

        // Fits into the first shelf, which still has horizontal space.
        assertEquals(pack64(40, 0), packing.allocate(20, 20))
    }

    @Test
    fun testShelfHeightCanGrow() {
        val packing = ShelfPacking(100, 100, 5)

        assertEquals(pack64(0, 0), packing.allocate(20, 10))

        // This is allowed by the tolerance and grows the shelf height.
        val result = packing.allocate(20, 15)

        assertEquals(pack64(20, 0), result)

        // The shelf is now 15 high.
        assertEquals(pack64(40, 0), packing.allocate(20, 15))
    }

    @Test
    fun testHeightTolerance() {
        val packing = ShelfPacking(
            width = 100,
            height = 100,
            heightTolerance = 5
        )

        assertEquals(
            pack64(0, 0),
            packing.allocate(20, 10)
        )

        // 14 <= 10 + 5, therefore it fits the existing shelf.
        assertEquals(
            pack64(20, 0),
            packing.allocate(20, 14)
        )

        // 16 > 14 + 5 is false; it still fits because the shelf has grown
        // to 14 and tolerance is 5.
        assertEquals(
            pack64(40, 0),
            packing.allocate(20, 16)
        )
    }

    @Test
    fun testHeightOverflow() {
        val packing = ShelfPacking(100, 50)

        assertEquals(
            pack64(0, 0),
            packing.allocate(100, 30)
        )

        assertEquals(
            pack64(0, 30),
            packing.allocate(100, 20)
        )

        assertEquals(
            -1L,
            packing.allocate(1, 1)
        )
    }

    @Test
    fun testWidthFragmentationStartsNewShelf() {
        val packing = ShelfPacking(100, 100)

        assertEquals(
            pack64(0, 0),
            packing.allocate(60, 10)
        )

        // Only 40px remain, so this cannot fit into the first shelf.
        assertEquals(
            pack64(0, 10),
            packing.allocate(50, 10)
        )
    }

    @Test
    fun testFirstFitShelfSelection() {
        val packing = ShelfPacking(100, 100)

        // Shelf 0: 60px used, 40px remaining.
        assertEquals(
            pack64(0, 0),
            packing.allocate(60, 10)
        )

        // Shelf 1: 30px used, 70px remaining.
        assertEquals(
            pack64(0, 10),
            packing.allocate(30, 20)
        )

        // Fits into shelf 0, so first-fit chooses it.
        assertEquals(
            pack64(60, 0),
            packing.allocate(40, 10)
        )
    }

    @Test
    fun testClear() {
        val packing = ShelfPacking(100, 100)

        packing.allocate(50, 20)
        packing.allocate(50, 20)
        packing.allocate(100, 30)

        packing.clear()

        assertEquals(
            pack64(0, 0),
            packing.allocate(50, 20)
        )
    }

    @Test
    fun testDeterministicPlacement() {
        fun createPacking(): List<Long> {
            val packing = ShelfPacking(100, 100)

            return listOf(
                packing.allocate(30, 10),
                packing.allocate(20, 20),
                packing.allocate(60, 10),
                packing.allocate(10, 30),
            )
        }

        assertEquals(createPacking(), createPacking())
    }

    @Test
    fun testFailedAllocationDoesNotModifyPacking() {
        val packing = ShelfPacking(100, 20)

        assertEquals(
            pack64(0, 0),
            packing.allocate(100, 20)
        )

        assertEquals(
            -1L,
            packing.allocate(1, 1)
        )

        // The failed allocation must not have changed the shelves.
        assertEquals(
            -1L,
            packing.allocate(1, 1)
        )
    }

    @Test
    fun testSinglePixelRectangles() {
        val packing = ShelfPacking(4, 4)

        assertEquals(pack64(0, 0), packing.allocate(1, 1))
        assertEquals(pack64(1, 0), packing.allocate(1, 1))
        assertEquals(pack64(2, 0), packing.allocate(1, 1))
        assertEquals(pack64(3, 0), packing.allocate(1, 1))

        assertEquals(pack64(0, 1), packing.allocate(1, 1))
    }

    @Test
    fun testFailedTooWideRectangleDoesNotCreateShelf() {
        val packing = ShelfPacking(10, 10)

        assertEquals(-1L, packing.allocate(11, 1))

        assertEquals(
            pack64(0, 0),
            packing.allocate(10, 1)
        )
    }

    @Test
    fun testSuccessfulAllocationsAreDifferentWhenExpected() {
        val packing = ShelfPacking(100, 100)

        val a = packing.allocate(50, 10)
        val b = packing.allocate(50, 10)
        val c = packing.allocate(1, 1)

        assertNotEquals(a, b)
        assertNotEquals(b, c)
    }


    @Test
    fun allocationsNeverOverlap() {
        data class Rect(
            val x: Int,
            val y: Int,
            val w: Int,
            val h: Int,
        )

        val width = 256
        val height = 256

        val packing = ShelfPacking(
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

            val packing = ShelfPacking(
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