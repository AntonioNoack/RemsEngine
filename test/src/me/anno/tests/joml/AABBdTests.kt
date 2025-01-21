package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.PI

class AABBdTests {

    @Test
    fun testMainConstructor() {
        val tested = AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(1.0, tested.minX)
        assertEquals(2.0, tested.minY)
        assertEquals(3.0, tested.minZ)
        assertEquals(4.0, tested.maxX)
        assertEquals(5.0, tested.maxY)
        assertEquals(6.0, tested.maxZ)
    }

    @Test
    fun testConstructors() {
        assertEquals(AABBd(1.0, 1.0, 1.0, 2.0, 2.0, 2.0), AABBd(1.0, 2.0))
        assertEquals(AABBd(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), AABBd())
        assertTrue(AABBd().isEmpty())
        assertEquals(
            AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
            AABBd(AABBf(1f, 2f, 3f, 4f, 5f, 6f))
        )
        assertEquals(6, AABBd().numComponents)
    }

    @Test
    fun testGetComponent() {
        val tested = AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(1.0, tested.getComp(0))
        assertEquals(2.0, tested.getComp(1))
        assertEquals(3.0, tested.getComp(2))
        assertEquals(4.0, tested.getComp(3))
        assertEquals(5.0, tested.getComp(4))
        assertEquals(6.0, tested.getComp(5))
    }

    @Test
    fun testSetComponent() {
        assertEquals(AABBd(1.0, 0.0, 0.0, 0.0, 0.0, 0.0), AABBd(0.0, 0.0).apply { setComp(0, 1.0) })
        assertEquals(AABBd(0.0, 2.0, 0.0, 0.0, 0.0, 0.0), AABBd(0.0, 0.0).apply { setComp(1, 2.0) })
        assertEquals(AABBd(0.0, 0.0, 3.0, 0.0, 0.0, 0.0), AABBd(0.0, 0.0).apply { setComp(2, 3.0) })
        assertEquals(AABBd(0.0, 0.0, 0.0, 4.0, 0.0, 0.0), AABBd(0.0, 0.0).apply { setComp(3, 4.0) })
        assertEquals(AABBd(0.0, 0.0, 0.0, 0.0, 5.0, 0.0), AABBd(0.0, 0.0).apply { setComp(4, 5.0) })
        assertEquals(AABBd(0.0, 0.0, 0.0, 0.0, 0.0, 6.0), AABBd(0.0, 0.0).apply { setComp(5, 6.0) })
    }

    @Test
    fun testToString() {
        assertEquals("(1.0,2.0,3.0)-(4.0,5.0,6.0)", AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).toString())
    }

    @Test
    fun testSetMinMax() {
        assertEquals(
            AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
            AABBd().setMin(1.0, 2.0, 3.0).setMax(4.0, 5.0, 6.0)
        )
        assertEquals(
            AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
            AABBd().setMin(Vector3d(1.0, 2.0, 3.0)).setMax(Vector3d(4.0, 5.0, 6.0))
        )
    }

    @Test
    fun testGetMinMax() {
        val tested = AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(1.0, tested.getMin(0))
        assertEquals(2.0, tested.getMin(1))
        assertEquals(3.0, tested.getMin(2))
        assertEquals(4.0, tested.getMax(0))
        assertEquals(5.0, tested.getMax(1))
        assertEquals(6.0, tested.getMax(2))
        assertEquals(Vector3d(1.0, 2.0, 3.0), tested.getMin())
        assertEquals(Vector3d(4.0, 5.0, 6.0), tested.getMax())
        assertEquals(2.5, tested.centerX)
        assertEquals(3.5, tested.centerY)
        assertEquals(4.5, tested.centerZ)
        assertEquals(Vector3d(2.5, 3.5, 4.5), tested.getCenter(Vector3d()))
        assertEquals(Vector3f(2.5f, 3.5f, 4.5f), tested.getCenter(Vector3f()))
    }

    @Test
    fun testUnion() {
        val a = AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val b = AABBd(2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
        val expected = AABBd(1.0, 2.0, 3.0, 5.0, 6.0, 7.0)
        assertEquals(expected, a.union(b, AABBd()))
        assertEquals(expected, b.union(a, AABBd()))
        assertNotEquals(expected, a)
        assertNotEquals(expected, b)
    }

    @Test
    fun testUnion2() {
        val a = AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val b = AABBf(2f, 3f, 4f, 5f, 6f, 7f)
        val expected = AABBd(1.0, 2.0, 3.0, 5.0, 6.0, 7.0)
        assertEquals(expected, a.union(b, AABBd()))
        assertNotEquals(expected, a)
        assertNotEquals(expected, b)
    }

    @Test
    fun testUnion3() {
        val a = AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val b = Vector3d(1.0, -3.0, 9.0)
        val expected = AABBd(1.0, -3.0, 3.0, 4.0, 5.0, 9.0)
        assertEquals(expected, a.union(b, AABBd()))
        assertNotEquals(expected, a)
    }

    @Test
    fun testClear() {
        assertEquals(AABBd(), AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).clear())
    }

    @Test
    fun testAll() {
        assertEquals(
            AABBd(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
            AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).all()
        )
    }

    @Test
    fun testTranslate() {
        assertEquals(
            AABBd(2.0, 4.0, 6.0, 5.0, 7.0, 9.0),
            AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).translate(1.0, 2.0, 3.0)
        )
    }

    @Test
    fun testAddMargin() {
        assertEquals(
            AABBd(-1.0, 0.0, 1.0, 6.0, 7.0, 8.0),
            AABBd(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).addMargin(2.0)
        )
    }

    @Test
    fun testTransform180deg() {
        assertEquals(
            AABBd(-1.0, -4.0, -5.0, 3.0, 2.0, 3.0),
            AABBd(-1.0, -2.0, -3.0, 3.0, 4.0, 5.0)
                .transform(Matrix4d().rotateX(PI)), 1e-15
        )
        assertEquals(
            AABBd(-3.0, -2.0, -5.0, 1.0, 4.0, 3.0),
            AABBd(-1.0, -2.0, -3.0, 3.0, 4.0, 5.0)
                .transform(Matrix4d().rotateY(PI)), 1e-15
        )
        assertEquals(
            AABBd(-3.0, -4.0, -3.0, 1.0, 2.0, 5.0),
            AABBd(-1.0, -2.0, -3.0, 3.0, 4.0, 5.0)
                .transform(Matrix4d().rotateZ(PI)), 1e-15
        )
    }

    @Test
    fun testTransform90deg() {
        assertEquals(
            AABBd(-1.0, -6.0, -2.0, 4.0, 3.0, 5.0),
            AABBd(-1.0, -2.0, -3.0, 4.0, 5.0, 6.0)
                .transform(Matrix4d().rotateX(PI / 2)), 1e-15
        )
        assertEquals(
            AABBd(-3.0, -2.0, -4.0, 6.0, 5.0, 1.0),
            AABBd(-1.0, -2.0, -3.0, 4.0, 5.0, 6.0)
                .transform(Matrix4d().rotateY(PI / 2)), 1e-15
        )
        assertEquals(
            AABBd(-5.0, -1.0, -3.0, 2.0, 4.0, 6.0),
            AABBd(-1.0, -2.0, -3.0, 4.0, 5.0, 6.0)
                .transform(Matrix4d().rotateZ(PI / 2)), 1e-15
        )
    }
}