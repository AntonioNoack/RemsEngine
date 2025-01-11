package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Vector2i
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4i
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class Vector4iTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector4i(0, 0, 0, 1), Vector4i())
        assertEquals(Vector4i(2, 2, 2, 2), Vector4i(2))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i(Vector3i(1, 2, 3), 4))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i(Vector4i(1, 2, 3, 4)))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i(Vector2i(1, 2), 3, 4))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i(intArrayOf(0, 1, 2, 3, 4), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector4i(2, 2, 2, 2), Vector4i().set(2))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i().set(1, 2, 3, 4))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i().set(Vector4i(1, 2, 3, 4)))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i().set(Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i().set(Vector3i(1, 2, 3), 4))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i().set(Vector2i(1, 2), 3, 4))
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i().set(intArrayOf(0, 1, 2, 3, 4), 1))
    }

    @Test
    fun testComponents() {
        val (x, y, z, w) = Vector4i(1, 2, 3, 4)
        assertEquals(1, x)
        assertEquals(2, y)
        assertEquals(3, z)
        assertEquals(4, w)
    }

    @Test
    fun testLengths() {
        val expected = 3.0 * 3.0 + 2.0 * 2.0 + 4.0 * 4.0 + 1.0 * 1.0
        assertEquals(sqrt(expected), Vector4i(3, 2, 4, -1).length())
        assertEquals(expected.toLong(), Vector4i(3, 2, 4, -1).lengthSquared())
    }

    @Test
    fun testDistances() {
        val expected = 2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0 + 1.0 * 1.0
        assertEquals(sqrt(expected), Vector4i(3, 2, 4, 1).distance(5, 3, 0, 2))
        assertEquals(sqrt(expected), Vector4i(3, 2, 4, 1).distance(Vector4i(5, 3, 0, 2)))
        assertEquals(expected.toLong(), Vector4i(3, 2, 4, 1).distanceSquared(5, 3, 0, 2))
        assertEquals(expected.toLong(), Vector4i(3, 2, 4, 1).distanceSquared(Vector4i(5, 3, 0, 2)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector4i(1, 2, 2, 2).minComponent())
        assertEquals(1, Vector4i(2, 1, 2, 2).minComponent())
        assertEquals(2, Vector4i(2, 2, 1, 2).minComponent())
        assertEquals(3, Vector4i(2, 2, 2, 1).minComponent())
        assertEquals(0, Vector4i(2, 1, 1, 1).maxComponent())
        assertEquals(1, Vector4i(1, 2, 1, 1).maxComponent())
        assertEquals(2, Vector4i(1, 1, 2, 1).maxComponent())
        assertEquals(3, Vector4i(1, 1, 1, 2).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1, Vector4i(1, 2, 2, 2).min())
        assertEquals(1, Vector4i(2, 1, 2, 2).min())
        assertEquals(1, Vector4i(2, 2, 1, 2).min())
        assertEquals(1, Vector4i(2, 2, 2, 1).min())
        assertEquals(2, Vector4i(2, 1, 1, 1).max())
        assertEquals(2, Vector4i(1, 2, 1, 1).max())
        assertEquals(2, Vector4i(1, 1, 2, 1).max())
        assertEquals(2, Vector4i(1, 1, 1, 2).max())
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector4i(1, 2, 3, 3), Vector4i(-1, 2, -3, 3).absolute())
    }

    @Test
    fun testZero() {
        assertEquals(Vector4i(0), Vector4i(3).zero())
    }

    @Test
    fun testGridDistance() {
        assertEquals(1 + 2 + 3 + 4L, Vector4i(1).gridDistance(Vector4i(2, 3, 4, 5)))
    }

    @Test
    fun testDot() {
        assertEquals(
            1 * 2 + 2 * 3 + 3 * 4 + 4 * 5L,
            Vector4i(1, 2, 3, 4).dot(Vector4i(2, 3, 4, 5))
        )
    }

    @Test
    fun testNegate() {
        assertEquals(Vector4i(1, 2, 3, 4), Vector4i(-1, -2, -3, -4).negate())
        assertEquals(Vector4i(-1, -2, -3, -4), Vector4i(1, 2, 3, 4).negate())
    }
}