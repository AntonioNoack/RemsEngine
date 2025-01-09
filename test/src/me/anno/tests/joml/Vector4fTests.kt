package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.junit.jupiter.api.Test

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
}