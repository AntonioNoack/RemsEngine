package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.AABBf
import org.joml.AABBi
import org.joml.Vector3i
import org.junit.jupiter.api.Test

class AABBiTests {

    @Test
    fun testMainConstructor() {
        val tested = AABBi(1, 2, 3, 4, 5, 6)
        assertEquals(1, tested.minX)
        assertEquals(2, tested.minY)
        assertEquals(3, tested.minZ)
        assertEquals(4, tested.maxX)
        assertEquals(5, tested.maxY)
        assertEquals(6, tested.maxZ)
    }

    @Test
    fun testConstructors() {
        assertEquals(AABBi(1, 1, 1, 2, 2, 2), AABBi(1, 2))
        assertEquals(AABBi(Int.MAX_VALUE, Int.MIN_VALUE), AABBi())
        assertTrue(AABBi().isEmpty())
        assertEquals(
            AABBi(1, 2, 3, 4, 5, 6),
            AABBi(AABBf(1f, 2f, 3f, 4f, 5f, 6f))
        )
        assertEquals(6, AABBi().numComponents)
    }

    @Test
    fun testGetComponent() {
        val tested = AABBi(1, 2, 3, 4, 5, 6)
        assertEquals(1.0, tested.getComp(0))
        assertEquals(2.0, tested.getComp(1))
        assertEquals(3.0, tested.getComp(2))
        assertEquals(4.0, tested.getComp(3))
        assertEquals(5.0, tested.getComp(4))
        assertEquals(6.0, tested.getComp(5))
    }

    @Test
    fun testSetComponent() {
        assertEquals(AABBi(1, 0, 0, 0, 0, 0), AABBi(0, 0).apply { setComp(0, 1.0) })
        assertEquals(AABBi(0, 2, 0, 0, 0, 0), AABBi(0, 0).apply { setComp(1, 2.0) })
        assertEquals(AABBi(0, 0, 3, 0, 0, 0), AABBi(0, 0).apply { setComp(2, 3.0) })
        assertEquals(AABBi(0, 0, 0, 4, 0, 0), AABBi(0, 0).apply { setComp(3, 4.0) })
        assertEquals(AABBi(0, 0, 0, 0, 5, 0), AABBi(0, 0).apply { setComp(4, 5.0) })
        assertEquals(AABBi(0, 0, 0, 0, 0, 6), AABBi(0, 0).apply { setComp(5, 6.0) })
    }

    @Test
    fun testToString() {
        assertEquals("(1,2,3)-(4,5,6)", AABBi(1, 2, 3, 4, 5, 6).toString())
    }

    @Test
    fun testSetMinMax() {
        assertEquals(
            AABBi(1, 2, 3, 4, 5, 6),
            AABBi().setMin(1, 2, 3).setMax(4, 5, 6)
        )
        assertEquals(
            AABBi(1, 2, 3, 4, 5, 6),
            AABBi().setMin(Vector3i(1, 2, 3)).setMax(Vector3i(4, 5, 6))
        )
    }

    @Test
    fun testGetMinMax() {
        val tested = AABBi(1, 2, 3, 4, 5, 6)
        assertEquals(1, tested.getMin(0))
        assertEquals(2, tested.getMin(1))
        assertEquals(3, tested.getMin(2))
        assertEquals(4, tested.getMax(0))
        assertEquals(5, tested.getMax(1))
        assertEquals(6, tested.getMax(2))
        assertEquals(Vector3i(1, 2, 3), tested.getMin())
        assertEquals(Vector3i(4, 5, 6), tested.getMax())
    }

    @Test
    fun testUnion() {
        val a = AABBi(1, 2, 3, 4, 5, 6)
        val b = AABBi(2, 3, 4, 5, 6, 7)
        val expected = AABBi(1, 2, 3, 5, 6, 7)
        assertEquals(expected, a.union(b, AABBi()))
        assertEquals(expected, b.union(a, AABBi()))
        assertNotEquals(expected, a)
        assertNotEquals(expected, b)
    }

    @Test
    fun testUnion2() {
        val a = AABBi(1, 2, 3, 4, 5, 6)
        val b = AABBf(2f, 3f, 4f, 5f, 6f, 7f)
        val expected = AABBi(1, 2, 3, 5, 6, 7)
        assertEquals(expected, a.union(b, AABBi()))
        assertNotEquals(expected, a)
        assertNotEquals(expected, b)
    }

    @Test
    fun testUnion3() {
        val a = AABBi(1, 2, 3, 4, 5, 6)
        val b = Vector3i(1, -3, 9)
        val expected = AABBi(1, -3, 3, 4, 5, 9)
        assertEquals(expected, a.union(b, AABBi()))
        assertNotEquals(expected, a)
    }

    @Test
    fun testClear() {
        assertEquals(AABBi(), AABBi(1, 2, 3, 4, 5, 6).clear())
    }

    @Test
    fun testAll() {
        assertEquals(AABBi(Int.MIN_VALUE, Int.MAX_VALUE), AABBi(1, 2, 3, 4, 5, 6).all())
    }

    @Test
    fun testAddMargin() {
        assertEquals(
            AABBi(-1, 0, 1, 6, 7, 8),
            AABBi(1, 2, 3, 4, 5, 6).addMargin(2)
        )
    }
}