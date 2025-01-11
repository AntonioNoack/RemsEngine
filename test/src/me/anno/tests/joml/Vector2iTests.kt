package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Vector2i
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class Vector2iTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector2i(0, 0), Vector2i())
        assertEquals(Vector2i(2, 2), Vector2i(2))
        assertEquals(Vector2i(1, 2), Vector2i(intArrayOf(0, 1, 2, 3, 4), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector2i(2, 2), Vector2i().set(2))
        assertEquals(Vector2i(1, 2), Vector2i().set(1, 2))
        assertEquals(Vector2i(1, 2), Vector2i().set(Vector2i(1, 2)))
        assertEquals(Vector2i(1, 2), Vector2i().set(intArrayOf(0, 1, 2, 3, 4), 1))
    }

    @Test
    fun testComponents() {
        val (x, y) = Vector2i(1, 2)
        assertEquals(1, x)
        assertEquals(2, y)
    }

    @Test
    fun testLengths() {
        val expected = 3.0 * 3.0 + 2.0 * 2.0
        assertEquals(sqrt(expected), Vector2i(3, 2).length())
        assertEquals(expected.toLong(), Vector2i(3, 2).lengthSquared())
    }

    @Test
    fun testDistances() {
        val expected = 2.0 * 2.0 + 1.0 * 1.0
        assertEquals(sqrt(expected), Vector2i(3, 2).distance(5, 3))
        assertEquals(sqrt(expected), Vector2i(3, 2).distance(Vector2i(5, 3)))
        assertEquals(expected.toLong(), Vector2i(3, 2).distanceSquared(5, 3))
        assertEquals(expected.toLong(), Vector2i(3, 2).distanceSquared(Vector2i(5, 3)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector2i(1, 2).minComponent())
        assertEquals(1, Vector2i(2, 1).minComponent())
        assertEquals(0, Vector2i(2, 1).maxComponent())
        assertEquals(1, Vector2i(1, 2).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1, Vector2i(1, 2).min())
        assertEquals(1, Vector2i(2, 1).min())
        assertEquals(2, Vector2i(2, 1).max())
        assertEquals(2, Vector2i(1, 2).max())
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector2i(1, 2), Vector2i(-1, 2).absolute())
        assertEquals(Vector2i(1, 2), Vector2i(1, -2).absolute())
    }

    @Test
    fun testZero() {
        assertEquals(Vector2i(0), Vector2i(3).zero())
    }

    @Test
    fun testGridDistance() {
        assertEquals(1 + 2L, Vector2i(1).gridDistance(Vector2i(2, 3)))
    }

    @Test
    fun testDot() {
        assertEquals(
            1 * 2 + 2 * 3L,
            Vector2i(1, 2).dot(Vector2i(2, 3))
        )
    }

    @Test
    fun testNegate() {
        assertEquals(Vector2i(1, 2), Vector2i(-1, -2).negate())
        assertEquals(Vector2i(-1, -2), Vector2i(1, 2).negate())
    }
}