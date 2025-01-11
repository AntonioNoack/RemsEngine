package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Vector2i
import org.joml.Vector3i
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class Vector3iTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector3i(0, 0, 0), Vector3i())
        assertEquals(Vector3i(2, 2, 2), Vector3i(2))
        assertEquals(Vector3i(1, 2, 3), Vector3i(Vector2i(1, 2), 3))
        assertEquals(Vector3i(1, 2, 3), Vector3i(intArrayOf(0, 1, 2, 3, 4), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector3i(2, 2, 2), Vector3i().set(2))
        assertEquals(Vector3i(1, 2, 3), Vector3i().set(1, 2, 3))
        assertEquals(Vector3i(1, 2, 3), Vector3i().set(Vector3i(1, 2, 3)))
        assertEquals(Vector3i(1, 2, 3), Vector3i().set(Vector2i(1, 2), 3))
        assertEquals(Vector3i(1, 2, 3), Vector3i().set(intArrayOf(0, 1, 2, 3, 4), 1))
    }

    @Test
    fun testComponents() {
        val (x, y, z) = Vector3i(1, 2, 3)
        assertEquals(1, x)
        assertEquals(2, y)
        assertEquals(3, z)
    }

    @Test
    fun testLengths() {
        val expected = 3.0 * 3.0 + 2.0 * 2.0 + 4.0 * 4.0
        assertEquals(sqrt(expected), Vector3i(3, 2, 4).length())
        assertEquals(expected.toLong(), Vector3i(3, 2, 4).lengthSquared())
    }

    @Test
    fun testDistances() {
        val expected = 2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0
        assertEquals(sqrt(expected), Vector3i(3, 2, 4).distance(5, 3, 0))
        assertEquals(sqrt(expected), Vector3i(3, 2, 4).distance(Vector3i(5, 3, 0)))
        assertEquals(expected.toLong(), Vector3i(3, 2, 4).distanceSquared(5, 3, 0))
        assertEquals(expected.toLong(), Vector3i(3, 2, 4).distanceSquared(Vector3i(5, 3, 0)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector3i(1, 2, 2).minComponent())
        assertEquals(1, Vector3i(2, 1, 2).minComponent())
        assertEquals(2, Vector3i(2, 2, 1).minComponent())
        assertEquals(0, Vector3i(2, 1, 1).maxComponent())
        assertEquals(1, Vector3i(1, 2, 1).maxComponent())
        assertEquals(2, Vector3i(1, 1, 2).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1, Vector3i(1, 2, 2).min())
        assertEquals(1, Vector3i(2, 1, 2).min())
        assertEquals(1, Vector3i(2, 2, 1).min())
        assertEquals(2, Vector3i(2, 1, 1).max())
        assertEquals(2, Vector3i(1, 2, 1).max())
        assertEquals(2, Vector3i(1, 1, 2).max())
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector3i(1, 2, 3), Vector3i(-1, 2, -3).absolute())
    }

    @Test
    fun testZero() {
        assertEquals(Vector3i(0), Vector3i(3).zero())
    }

    @Test
    fun testGridDistance() {
        assertEquals(1 + 2 + 3L, Vector3i(1).gridDistance(Vector3i(2, 3, 4)))
    }

    @Test
    fun testDot() {
        assertEquals(
            1 * 2 + 2 * 3 + 3 * 4L,
            Vector3i(1, 2, 3).dot(Vector3i(2, 3, 4))
        )
    }

    @Test
    fun testNegate() {
        assertEquals(Vector3i(1, 2, 3), Vector3i(-1, -2, -3).negate())
        assertEquals(Vector3i(-1, -2, -3), Vector3i(1, 2, 3).negate())
    }
}