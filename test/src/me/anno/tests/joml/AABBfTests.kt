package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.AABBf
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class AABBfTests {

    @Test
    fun testMainConstructor() {
        val tested = AABBf(1f, 2f, 3f, 4f, 5f, 6f)
        assertEquals(1f, tested.minX)
        assertEquals(2f, tested.minY)
        assertEquals(3f, tested.minZ)
        assertEquals(4f, tested.maxX)
        assertEquals(5f, tested.maxY)
        assertEquals(6f, tested.maxZ)
    }

    @Test
    fun testConstructors() {
        assertEquals(AABBf(1f, 1f, 1f, 2f, 2f, 2f), AABBf(1f, 2f))
        assertEquals(AABBf(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY), AABBf())
        assertTrue(AABBf().isEmpty())
        assertEquals(
            AABBf(1f, 2f, 3f, 4f, 5f, 6f),
            AABBf(AABBf(1f, 2f, 3f, 4f, 5f, 6f))
        )
        assertEquals(6, AABBf().numComponents)
    }

    @Test
    fun testGetComponent() {
        val tested = AABBf(1f, 2f, 3f, 4f, 5f, 6f)
        assertEquals(1.0, tested.getComp(0))
        assertEquals(2.0, tested.getComp(1))
        assertEquals(3.0, tested.getComp(2))
        assertEquals(4.0, tested.getComp(3))
        assertEquals(5.0, tested.getComp(4))
        assertEquals(6.0, tested.getComp(5))
    }

    @Test
    fun testSetComponent() {
        assertEquals(AABBf(1f, 0f, 0f, 0f, 0f, 0f), AABBf(0f, 0f).apply { setComp(0, 1.0) })
        assertEquals(AABBf(0f, 2f, 0f, 0f, 0f, 0f), AABBf(0f, 0f).apply { setComp(1, 2.0) })
        assertEquals(AABBf(0f, 0f, 3f, 0f, 0f, 0f), AABBf(0f, 0f).apply { setComp(2, 3.0) })
        assertEquals(AABBf(0f, 0f, 0f, 4f, 0f, 0f), AABBf(0f, 0f).apply { setComp(3, 4.0) })
        assertEquals(AABBf(0f, 0f, 0f, 0f, 5f, 0f), AABBf(0f, 0f).apply { setComp(4, 5.0) })
        assertEquals(AABBf(0f, 0f, 0f, 0f, 0f, 6f), AABBf(0f, 0f).apply { setComp(5, 6.0) })
    }

    @Test
    fun testToString() {
        assertEquals("(1.0,2.0,3.0)-(4.0,5.0,6.0)", AABBf(1f, 2f, 3f, 4f, 5f, 6f).toString())
    }

    @Test
    fun testSetMinMax() {
        assertEquals(
            AABBf(1f, 2f, 3f, 4f, 5f, 6f),
            AABBf().setMin(1f, 2f, 3f).setMax(4f, 5f, 6f)
        )
        assertEquals(
            AABBf(1f, 2f, 3f, 4f, 5f, 6f),
            AABBf().setMin(Vector3f(1f, 2f, 3f)).setMax(Vector3f(4f, 5f, 6f))
        )
    }

    @Test
    fun testGetMinMax() {
        val tested = AABBf(1f, 2f, 3f, 4f, 5f, 6f)
        assertEquals(1f, tested.getMin(0))
        assertEquals(2f, tested.getMin(1))
        assertEquals(3f, tested.getMin(2))
        assertEquals(4f, tested.getMax(0))
        assertEquals(5f, tested.getMax(1))
        assertEquals(6f, tested.getMax(2))
        assertEquals(Vector3f(1f, 2f, 3f), tested.getMin())
        assertEquals(Vector3f(4f, 5f, 6f), tested.getMax())
    }

    @Test
    fun testUnion() {
        val a = AABBf(1f, 2f, 3f, 4f, 5f, 6f)
        val b = AABBf(2f, 3f, 4f, 5f, 6f, 7f)
        val expected = AABBf(1f, 2f, 3f, 5f, 6f, 7f)
        assertEquals(expected, a.union(b, AABBf()))
        assertEquals(expected, b.union(a, AABBf()))
        assertNotEquals(expected, a)
        assertNotEquals(expected, b)
    }

    @Test
    fun testUnion2() {
        val a = AABBf(1f, 2f, 3f, 4f, 5f, 6f)
        val b = AABBf(2f, 3f, 4f, 5f, 6f, 7f)
        val expected = AABBf(1f, 2f, 3f, 5f, 6f, 7f)
        assertEquals(expected, a.union(b, AABBf()))
        assertNotEquals(expected, a)
        assertNotEquals(expected, b)
    }

    @Test
    fun testUnion3() {
        val a = AABBf(1f, 2f, 3f, 4f, 5f, 6f)
        val b = Vector3f(1f, -3f, 9f)
        val expected = AABBf(1f, -3f, 3f, 4f, 5f, 9f)
        assertEquals(expected, a.union(b, AABBf()))
        assertNotEquals(expected, a)
    }

    @Test
    fun testClear() {
        assertEquals(AABBf(), AABBf(1f, 2f, 3f, 4f, 5f, 6f).clear())
    }

    @Test
    fun testAll() {
        assertEquals(
            AABBf(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY),
            AABBf(1f, 2f, 3f, 4f, 5f, 6f).all()
        )
    }

    @Test
    fun testTranslate() {
        assertEquals(
            AABBf(2f, 4f, 6f, 5f, 7f, 9f),
            AABBf(1f, 2f, 3f, 4f, 5f, 6f).translate(1f, 2f, 3f)
        )
    }

    @Test
    fun testAddMargin() {
        assertEquals(
            AABBf(-1f, 0f, 1f, 6f, 7f, 8f),
            AABBf(1f, 2f, 3f, 4f, 5f, 6f).addMargin(2f)
        )
    }
}