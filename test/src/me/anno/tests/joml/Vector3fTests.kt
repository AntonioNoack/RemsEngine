package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.junit.jupiter.api.Test
import kotlin.math.round
import kotlin.math.sqrt

class Vector3fTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector3f(1f, 1f, 1f), Vector3f(1f))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(1, 2, 3))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(1f, 2f, 3f))
        assertEquals(Vector3f(0f, 0f, 0f), Vector3f())
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(Vector3i(1, 2, 3)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(Vector3d(1.0, 2.0, 3.0)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(floatArrayOf(1f, 2f, 3f)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(floatArrayOf(5f, 1f, 2f, 3f), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector3f(0f, 0f, 0f), Vector3f())
        assertEquals(Vector3f(1f, 1f, 1f), Vector3f().set(1f))
        assertEquals(Vector3f(1f, 1f, 1f), Vector3f().set(1.0))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f().set(1.0, 2.0, 3.0))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f().set(Vector3i(1, 2, 3)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f().set(Vector3f(1f, 2f, 3f)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f().set(Vector3d(1.0, 2.0, 3.0)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f().set(floatArrayOf(1f, 2f, 3f)))
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f().set(floatArrayOf(5f, 1f, 2f, 3f), 1))
    }

    @Test
    fun testComponents() {
        val (x, y, z) = Vector3f(1f, 2f, 3f)
        assertEquals(1f, x)
        assertEquals(2f, y)
        assertEquals(3f, z)
    }

    @Test
    fun testLengths() {
        assertEquals(sqrt(3f * 3f + 2f * 2f + 4f * 4f), Vector3f(3f, 2f, 4f).length())
        assertEquals(3f * 3f + 2f * 2f + 4f * 4f, Vector3f(3f, 2f, 4f).lengthSquared())
    }

    @Test
    fun testDistances() {
        assertEquals(sqrt(2f * 2f + 1f * 1f + 4f * 4f), Vector3f(3f, 2f, 4f).distance(5f, 3f, 0f))
        assertEquals(sqrt(2f * 2f + 1f * 1f + 4f * 4f), Vector3f(3f, 2f, 4f).distance(Vector3f(5f, 3f, 0f)))
        assertEquals(2f * 2f + 1f * 1f + 4f * 4f, Vector3f(3f, 2f, 4f).distanceSquared(5f, 3f, 0f))
        assertEquals(2f * 2f + 1f * 1f + 4f * 4f, Vector3f(3f, 2f, 4f).distanceSquared(Vector3f(5f, 3f, 0f)))
    }

    @Test
    fun testNormalize() {
        val a = Vector3f(3f, 5f, 4f)
        val b = a.normalize(Vector3f())
        assertNotEquals(a, b)
        assertEquals(1f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b), 1e-3f)
    }

    @Test
    fun testNormalize2() {
        val a = Vector3f(3f, 4f, 5f)
        val b = a.normalize(2f, Vector3f())
        assertNotEquals(a, b)
        assertEquals(2f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b))
    }

    @Test
    fun testSafeNormalize() {
        val a = Vector3f(3f, 5f, 4f)
        val b = Vector3f(a).safeNormalize()
        assertNotEquals(a, b)
        assertEquals(1f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b), 1e-3f)
        assertEquals(Vector3f(), Vector3f().safeNormalize())
    }

    @Test
    fun testFma() {
        assertEquals(Vector3f(3f, 5f, 7f), Vector3f(1f).fma(2f, Vector3f(1f, 2f, 3f)))
        assertEquals(Vector3f(3f, 7f, 17f), Vector3f(1f).fma(Vector3f(2f, 3f, 4f), Vector3f(1f, 2f, 4f)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector3f(1f, 2f, 2f).minComponent())
        assertEquals(1, Vector3f(2f, 1f, 2f).minComponent())
        assertEquals(2, Vector3f(2f, 2f, 1f).minComponent())
        assertEquals(0, Vector3f(2f, 1f, 1f).maxComponent())
        assertEquals(1, Vector3f(1f, 2f, 1f).maxComponent())
        assertEquals(2, Vector3f(1f, 1f, 2f).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1f, Vector3f(1f, 2f, 2f).min())
        assertEquals(1f, Vector3f(2f, 1f, 2f).min())
        assertEquals(1f, Vector3f(2f, 2f, 1f).min())
        assertEquals(2f, Vector3f(2f, 1f, 1f).max())
        assertEquals(2f, Vector3f(1f, 2f, 1f).max())
        assertEquals(2f, Vector3f(1f, 1f, 2f).max())
    }

    @Test
    fun testFloorRoundCeil() {
        assertEquals(Vector3f(1f, 2f, 1f), Vector3f(1.3f, 2.9f, 1.7f).floor())
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(1.3f, 1.9f, 2.6f).round())
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(0.3f, 1.9f, 2.1f).ceil())
    }

    @Test
    fun testIsFinite() {
        assertTrue(Vector3f(1e38f).isFinite)
        assertFalse(Vector3f(0f, Float.NaN, 0f).isFinite)
        assertFalse(Vector3f(Float.POSITIVE_INFINITY, 1f, 5f).isFinite)
        assertFalse(Vector3f(1f, -5f, Float.NEGATIVE_INFINITY).isFinite)
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(-1f, 2f, -3f).absolute())
    }

    @Test
    fun testCross() {
        assertEquals(Vector3f(-1f, 2f, -1f), Vector3f(1f, 2f, 3f).cross(3f, 5f, 7f))
        assertEquals(Vector3f(-1f, 2f, -1f), Vector3f(1f, 2f, 3f).cross(Vector3f(3f, 5f, 7f)))
    }

    @Test
    fun testMulAdd() {
        assertEquals(Vector3f(8f, 13f, 18f), Vector3f(1f, 2f, 3f).mulAdd(3f, Vector3f(5f, 7f, 9f), Vector3f()))
    }
}