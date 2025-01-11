package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class Vector4fTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector4f(0f, 0f, 0f, 1f), Vector4f())
        assertEquals(Vector4f(2f, 2f, 2f, 2f), Vector4f(2f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector4f(1f, 2f, 3f, 4f)))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector4i(1, 2, 3, 4)))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector3f(1f, 2f, 3f), 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector3i(1, 2, 3), 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector2f(1f, 2f), 3f, 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(Vector2i(1, 2), 3f, 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f(floatArrayOf(0f, 1f, 2f, 3f, 4f), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector4f(2f, 2f, 2f, 2f), Vector4f().set(2f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(1f, 2f, 3f, 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(1.0, 2.0, 3.0, 4.0))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector4f(1f, 2f, 3f, 4f)))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector4i(1, 2, 3, 4)))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector3f(1f, 2f, 3f), 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector3i(1, 2, 3), 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector2f(1f, 2f), 3f, 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(Vector2i(1, 2), 3f, 4f))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), Vector4f().set(floatArrayOf(0f, 1f, 2f, 3f, 4f), 1))
    }

    @Test
    fun testComponents() {
        val (x, y, z, w) = Vector4f(1f, 2f, 3f, 4f)
        assertEquals(1f, x)
        assertEquals(2f, y)
        assertEquals(3f, z)
        assertEquals(4f, w)
    }

    @Test
    fun testLengths() {
        val expected = 3f * 3f + 2f * 2f + 4f * 4f + 1f * 1f
        assertEquals(sqrt(expected), Vector4f(3f, 2f, 4f, -1f).length())
        assertEquals(expected, Vector4f(3f, 2f, 4f, -1f).lengthSquared())
        val expected3 = 3f * 3f + 2f * 2f + 4f * 4f
        assertEquals(sqrt(expected3), Vector4f(3f, 2f, 4f, -1f).length3())
    }

    @Test
    fun testDistances() {
        val expected = 2f * 2f + 1f * 1f + 4f * 4f + 1f * 1f
        assertEquals(sqrt(expected), Vector4f(3f, 2f, 4f, 1f).distance(5f, 3f, 0f, 2f))
        assertEquals(sqrt(expected), Vector4f(3f, 2f, 4f, 1f).distance(Vector4f(5f, 3f, 0f, 2f)))
        assertEquals(expected, Vector4f(3f, 2f, 4f, 1f).distanceSquared(5f, 3f, 0f, 2f))
        assertEquals(expected, Vector4f(3f, 2f, 4f, 1f).distanceSquared(Vector4f(5f, 3f, 0f, 2f)))
    }

    @Test
    fun testNormalize() {
        val a = Vector4f(3f, 5f, 4f, 1f)
        val b = a.normalize(Vector4f())
        assertNotEquals(a, b)
        assertEquals(1f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b), 1e-3f)
    }

    @Test
    fun testNormalize2() {
        val a = Vector4f(3f, 4f, 5f, 1f)
        val b = a.normalize(2f, Vector4f())
        assertNotEquals(a, b)
        assertEquals(2f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b))
    }

    @Test
    fun testSafeNormalize() {
        val a = Vector4f(3f, 5f, 4f, 1f)
        val b = Vector4f(a).safeNormalize()
        assertNotEquals(a, b)
        assertEquals(1f, b.length(), 1e-6f)
        assertEquals(0f, a.angle(b), 1e-3f)
        assertEquals(Vector4f(), Vector4f().safeNormalize())
    }

    @Test
    fun testFma() {
        assertEquals(Vector4f(3f, 5f, 7f, 9f), Vector4f(1f).fma(2f, Vector4f(1f, 2f, 3f, 4f)))
        assertEquals(Vector4f(3f, 7f, 17f, 36f), Vector4f(1f).fma(Vector4f(2f, 3f, 4f, 5f), Vector4f(1f, 2f, 4f, 7f)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector4f(1f, 2f, 2f, 2f).minComponent())
        assertEquals(1, Vector4f(2f, 1f, 2f, 2f).minComponent())
        assertEquals(2, Vector4f(2f, 2f, 1f, 2f).minComponent())
        assertEquals(3, Vector4f(2f, 2f, 2f, 1f).minComponent())
        assertEquals(0, Vector4f(2f, 1f, 1f, 1f).maxComponent())
        assertEquals(1, Vector4f(1f, 2f, 1f, 1f).maxComponent())
        assertEquals(2, Vector4f(1f, 1f, 2f, 1f).maxComponent())
        assertEquals(3, Vector4f(1f, 1f, 1f, 2f).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1f, Vector4f(1f, 2f, 2f, 2f).min())
        assertEquals(1f, Vector4f(2f, 1f, 2f, 2f).min())
        assertEquals(1f, Vector4f(2f, 2f, 1f, 2f).min())
        assertEquals(1f, Vector4f(2f, 2f, 2f, 1f).min())
        assertEquals(2f, Vector4f(2f, 1f, 1f, 1f).max())
        assertEquals(2f, Vector4f(1f, 2f, 1f, 1f).max())
        assertEquals(2f, Vector4f(1f, 1f, 2f, 1f).max())
        assertEquals(2f, Vector4f(1f, 1f, 1f, 2f).max())
    }

    @Test
    fun testFloorRoundCeil() {
        assertEquals(Vector4f(1f, 2f, 1f, -1f), Vector4f(1.3f, 2.9f, 1.7f, -0.2f).floor())
        assertEquals(Vector4f(1f, 2f, 3f, 0f), Vector4f(1.3f, 1.9f, 2.6f, -0.1f).round())
        assertEquals(Vector4f(1f, 2f, 3f, 0f), Vector4f(0.3f, 1.9f, 2.1f, -0.9f).ceil())
    }

    @Test
    fun testIsFinite() {
        assertTrue(Vector4f(1e38f).isFinite)
        assertFalse(Vector4f(0f, Float.NaN, 0f, 5f).isFinite)
        assertFalse(Vector4f(Float.POSITIVE_INFINITY, 1f, 5f, 1f).isFinite)
        assertFalse(Vector4f(1f, -5f, Float.NEGATIVE_INFINITY, 7f).isFinite)
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector4f(1f, 2f, 3f, 3f), Vector4f(-1f, 2f, -3f, 3f).absolute())
    }

    @Test
    fun testMulAdd() {
        assertEquals(
            Vector4f(8f, 13f, 18f, 1f),
            Vector4f(1f, 2f, 3f, 4f).mulAdd3(3f, Vector4f(5f, 7f, 9f, 11f), Vector4f())
        )
        assertEquals(
            Vector4f(8f, 13f, 18f, 23f),
            Vector4f(1f, 2f, 3f, 4f).mulAdd(3f, Vector4f(5f, 7f, 9f, 11f), Vector4f())
        )
    }
}