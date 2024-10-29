package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sqrt

class Vector2dTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector2d(1.0, 1.0), Vector2d(1.0))
        assertEquals(Vector2d(0.0, 0.0), Vector2d())
        assertEquals(Vector2d(1.0, 2.0), Vector2d(1.0, 2.0))
        assertEquals(Vector2d(1.0, 2.0), Vector2d(Vector2i(1, 2)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d(Vector2f(1.0, 2.0)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d(Vector2d(1.0, 2.0)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d(floatArrayOf(1f, 2f)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d(doubleArrayOf(1.0, 2.0)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d(doubleArrayOf(3.0, 1.0, 2.0), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector2d(0.0, 0.0), Vector2d())
        assertEquals(Vector2d(1.0, 1.0), Vector2d().set(1.0))
        assertEquals(Vector2d(1.0, 1.0), Vector2d().set(1.0))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(1.0, 2.0))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(Vector2i(1, 2)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(Vector2f(1.0, 2.0)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(Vector2d(1.0, 2.0)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(floatArrayOf(1f, 2f)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(doubleArrayOf(1.0, 2.0)))
        assertEquals(Vector2d(1.0, 2.0), Vector2d().set(doubleArrayOf(3.0, 1.0, 2.0), 1))
    }

    @Test
    fun testComponents() {
        val (x, y) = Vector2d(1.0, 2.0)
        assertEquals(1.0, x)
        assertEquals(2.0, y)
    }

    @Test
    fun testPerpendicular() {
        val a = Vector2d(3.0, 5.0)
        val b = Vector2d(a).perpendicular()
        assertEquals(0.0, a.dot(b))
        assertNotEquals(a, b)
        assertEquals(PI * 0.5, b.angle(a), 1e-7)
    }

    @Test
    fun testRotate() {
        val a = Vector2d(3.0, 2.0)
        for (i in -31..31) {
            val angle = i * 0.1
            val b = a.rotate(angle, Vector2d())
            assertEquals(angle, a.angle(b), 1e-6)
        }
    }

    @Test
    fun testLengths() {
        assertEquals(sqrt(3f * 3f + 2f * 2.0), Vector2d(3.0, 2.0).length())
        assertEquals(3f * 3f + 2f * 2.0, Vector2d(3.0, 2.0).lengthSquared())
    }

    @Test
    fun testDistances() {
        assertEquals(sqrt(2f * 2f + 1f * 1.0), Vector2d(3.0, 2.0).distance(5.0, 3.0))
        assertEquals(sqrt(2f * 2f + 1f * 1.0), Vector2d(3.0, 2.0).distance(Vector2d(5.0, 3.0)))
        assertEquals(2f * 2f + 1f * 1.0, Vector2d(3.0, 2.0).distanceSquared(5.0, 3.0))
        assertEquals(2f * 2f + 1f * 1.0, Vector2d(3.0, 2.0).distanceSquared(Vector2d(5.0, 3.0)))
    }

    @Test
    fun testNormalize() {
        val a = Vector2d(3.0, 5.0)
        val b = a.normalize(Vector2d())
        assertNotEquals(a, b)
        assertEquals(1.0, b.length(), 1e-15)
        assertEquals(0.0, a.angle(b))
    }

    @Test
    fun testNormalize2() {
        val a = Vector2d(3.0, 4.0)
        val b = a.normalize(2.0, Vector2d())
        assertNotEquals(a, b)
        assertEquals(2.0, b.length(), 1e-15)
        assertEquals(0.0, a.angle(b))
    }

    @Test
    fun testSafeNormalize() {
        val a = Vector2d(3.0, 5.0)
        val b = Vector2d(a).safeNormalize()
        assertNotEquals(a, b)
        assertEquals(1.0, b.length(), 1e-15)
        assertEquals(0.0, a.angle(b))
        assertEquals(Vector2d(), Vector2d().safeNormalize())
    }

    @Test
    fun testFma() {
        assertEquals(Vector2d(3.0, 5.0), Vector2d(1.0).fma(2.0, Vector2d(1.0, 2.0)))
        assertEquals(Vector2d(3.0, 7.0), Vector2d(1.0).fma(Vector2d(2.0, 3.0), Vector2d(1.0, 2.0)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector2d(1.0, 2.0).minComponent())
        assertEquals(1, Vector2d(1.0, 2.0).maxComponent())
        assertEquals(1, Vector2d(2.0, 1.0).minComponent())
        assertEquals(0, Vector2d(2.0, 1.0).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1.0, Vector2d(1.0, 2.0).min())
        assertEquals(2.0, Vector2d(1.0, 2.0).max())
        assertEquals(1.0, Vector2d(2.0, 1.0).min())
        assertEquals(2.0, Vector2d(2.0, 1.0).max())
    }

    @Test
    fun testFloorRoundCeil() {
        assertEquals(Vector2d(1.0, 2.0), Vector2d(1.3, 2.9).floor())
        assertEquals(Vector2d(1.0, 2.0), Vector2d(1.3, 1.9).round())
        assertEquals(Vector2d(1.0, 2.0), Vector2d(0.3, 1.9).ceil())
    }

    @Test
    fun testIsFinite() {
        assertTrue(Vector2d(1e38).isFinite)
        assertFalse(Vector2d(0.0, Double.NaN).isFinite)
        assertFalse(Vector2d(Double.POSITIVE_INFINITY, 1.0).isFinite)
        assertFalse(Vector2d(Double.NEGATIVE_INFINITY, 1.0).isFinite)
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector2d(1.0, 2.0), Vector2d(-1.0, 2.0).absolute())
    }

    @Test
    fun testCross() {
        assertEquals(-1.0, Vector2d(1.0, 2.0).cross(3.0, 5.0))
        assertEquals(-1.0, Vector2d(1.0, 2.0).cross(Vector2d(3.0, 5.0)))
    }

    @Test
    fun testMulAdd() {
        assertEquals(Vector2d(8.0, 13.0), Vector2d(1.0, 2.0).mulAdd(3.0, Vector2d(5.0, 7.0), Vector2d()))
    }

    @Test
    fun testMakePerpendicular() {
        assertEquals(Vector2d(0.0, 2.0), Vector2d(1.0, 2.0).makePerpendicular(Vector2d(5.0, 0.0)))
        assertEquals(Vector2d(0.0, 2.0), Vector2d(1.0, 2.0).makePerpendicular(Vector2d(-5.0, 0.0)))
    }
}