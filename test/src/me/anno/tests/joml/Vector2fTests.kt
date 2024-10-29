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

class Vector2fTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector2f(1f, 1f), Vector2f(1f))
        assertEquals(Vector2f(1f, 2f), Vector2f(1, 2))
        assertEquals(Vector2f(0f, 0f), Vector2f())
        assertEquals(Vector2f(1f, 2f), Vector2f(Vector2i(1, 2)))
        assertEquals(Vector2f(1f, 2f), Vector2f(1.0, 2.0))
        assertEquals(Vector2f(1f, 2f), Vector2f(Vector2d(1.0, 2.0)))
        assertEquals(Vector2f(1f, 2f), Vector2f(floatArrayOf(1f, 2f)))
        assertEquals(Vector2f(1f, 2f), Vector2f(floatArrayOf(3f, 1f, 2f), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector2f(0f, 0f), Vector2f())
        assertEquals(Vector2f(1f, 1f), Vector2f().set(1f))
        assertEquals(Vector2f(1f, 1f), Vector2f().set(1.0))
        assertEquals(Vector2f(1f, 2f), Vector2f().set(1.0, 2.0))
        assertEquals(Vector2f(1f, 2f), Vector2f().set(Vector2i(1, 2)))
        assertEquals(Vector2f(1f, 2f), Vector2f().set(Vector2f(1f, 2f)))
        assertEquals(Vector2f(1f, 2f), Vector2f().set(Vector2d(1.0, 2.0)))
        assertEquals(Vector2f(1f, 2f), Vector2f().set(floatArrayOf(1f, 2f)))
        assertEquals(Vector2f(1f, 2f), Vector2f().set(floatArrayOf(3f, 1f, 2f), 1))
    }

    @Test
    fun testComponents() {
        val (x, y) = Vector2f(1f, 2f)
        assertEquals(1f, x)
        assertEquals(2f, y)
    }

    @Test
    fun testPerpendicular() {
        val a = Vector2f(3f, 5f)
        val b = Vector2f(a).perpendicular()
        assertEquals(0f, a.dot(b))
        assertNotEquals(a, b)
        assertEquals(PI * 0.5, b.angle(a).toDouble(), 1e-7)
    }

    @Test
    fun testRotate() {
        val a = Vector2f(3f, 2f)
        for (i in -31..31) {
            val angle = i * 0.1f
            val b = a.rotate(angle, Vector2f())
            assertEquals(angle, a.angle(b), 1e-6f)
        }
    }

    @Test
    fun testLengths() {
        assertEquals(sqrt(3f * 3f + 2f * 2f), Vector2f(3f, 2f).length())
        assertEquals(3f * 3f + 2f * 2f, Vector2f(3f, 2f).lengthSquared())
    }

    @Test
    fun testDistances() {
        assertEquals(sqrt(2f * 2f + 1f * 1f), Vector2f(3f, 2f).distance(5f, 3f))
        assertEquals(sqrt(2f * 2f + 1f * 1f), Vector2f(3f, 2f).distance(Vector2f(5f, 3f)))
        assertEquals(2f * 2f + 1f * 1f, Vector2f(3f, 2f).distanceSquared(5f, 3f))
        assertEquals(2f * 2f + 1f * 1f, Vector2f(3f, 2f).distanceSquared(Vector2f(5f, 3f)))
    }

    @Test
    fun testNormalize() {
        val a = Vector2f(3f, 5f)
        val b = a.normalize(Vector2f())
        assertNotEquals(a, b)
        assertEquals(1f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b))
    }

    @Test
    fun testNormalize2() {
        val a = Vector2f(3f, 4f)
        val b = a.normalize(2f, Vector2f())
        assertNotEquals(a, b)
        assertEquals(2f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b))
    }

    @Test
    fun testSafeNormalize() {
        val a = Vector2f(3f, 5f)
        val b = Vector2f(a).safeNormalize()
        assertNotEquals(a, b)
        assertEquals(1f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b))
        assertEquals(Vector2f(), Vector2f().safeNormalize())
    }

    @Test
    fun testFma() {
        assertEquals(Vector2f(3f, 5f), Vector2f(1f).fma(2f, Vector2f(1f, 2f)))
        assertEquals(Vector2f(3f, 7f), Vector2f(1f).fma(Vector2f(2f, 3f), Vector2f(1f, 2f)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector2f(1f, 2f).minComponent())
        assertEquals(1, Vector2f(1f, 2f).maxComponent())
        assertEquals(1, Vector2f(2f, 1f).minComponent())
        assertEquals(0, Vector2f(2f, 1f).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1f, Vector2f(1f, 2f).min())
        assertEquals(2f, Vector2f(1f, 2f).max())
        assertEquals(1f, Vector2f(2f, 1f).min())
        assertEquals(2f, Vector2f(2f, 1f).max())
    }

    @Test
    fun testFloorRoundCeil() {
        assertEquals(Vector2f(1f, 2f), Vector2f(1.3f, 2.9f).floor())
        assertEquals(Vector2f(1f, 2f), Vector2f(1.3f, 1.9f).round())
        assertEquals(Vector2f(1f, 2f), Vector2f(0.3f, 1.9f).ceil())
    }

    @Test
    fun testIsFinite() {
        assertTrue(Vector2f(1e38f).isFinite)
        assertFalse(Vector2f(0f, Float.NaN).isFinite)
        assertFalse(Vector2f(Float.POSITIVE_INFINITY, 1f).isFinite)
        assertFalse(Vector2f(Float.NEGATIVE_INFINITY, 1f).isFinite)
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector2f(1f, 2f), Vector2f(-1f, 2f).absolute())
    }

    @Test
    fun testCross() {
        assertEquals(-1f, Vector2f(1f, 2f).cross(3f, 5f))
        assertEquals(-1f, Vector2f(1f, 2f).cross(Vector2f(3f, 5f)))
    }

    @Test
    fun testMulAdd() {
        assertEquals(Vector2f(8f, 13f), Vector2f(1f, 2f).mulAdd(3f, Vector2f(5f, 7f), Vector2f()))
    }

    @Test
    fun testMakePerpendicular() {
        assertEquals(Vector2f(0f, 2f), Vector2f(1f, 2f).makePerpendicular(Vector2f(5f, 0f)))
        assertEquals(Vector2f(0f, 2f), Vector2f(1f, 2f).makePerpendicular(Vector2f(-5f, 0f)))
    }
}