package me.anno.tests.joml

import me.anno.maths.Maths.PIf
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.joml.Matrix2f
import org.joml.Matrix3f
import org.joml.Vector2f
import org.junit.jupiter.api.Test

class Matrix2fTests {
    @Test
    fun testConstructors() {
        assertEquals(
            Matrix2f(1f, 2f, 3f, 4f),
            Matrix2f(Matrix2f(1f, 2f, 3f, 4f))
        )
        assertEquals(
            Matrix2f(1f, 2f, 3f, 4f),
            Matrix2f(Matrix3f(1f, 2f, 0f, 3f, 4f, 0f, 0f, 0f, 1f))
        )
        assertEquals(
            Matrix2f(1f, 2f, 3f, 4f),
            Matrix2f(Vector2f(1f, 2f), Vector2f(3f, 4f))
        )
        assertEquals(
            Matrix2f(),
            Matrix2f(1f, 0f, 0f, 1f)
        )
    }

    @Test
    fun testScaling() {
        assertEquals(
            Matrix2f(3f, 0f, 0f, 5f),
            Matrix2f().scaling(3f, 5f)
        )
    }

    @Test
    fun testScale() {
        assertEquals(
            Matrix2f(0f, 3f, -5f, 0f),
            Matrix2f().rotate(PIf * 0.5f).scale(3f, 5f),
            1e-6
        )
    }

    @Test
    fun testScaleLocal() {
        assertEquals(
            Matrix2f(0f, 5f, -3f, 0f),
            Matrix2f().rotate(PIf * 0.5f).scaleLocal(3f, 5f),
            1e-6
        )
    }

    @Test
    fun testRotate() {
        assertEquals(
            Matrix2f(0f, 3f, -5f, 0f),
            Matrix2f(5f, 0f, 0f, 3f).rotate(PIf * 0.5f),
            1e-6
        )
        assertEquals(
            Matrix2f(-5f, 0f, 0f, -3f),
            Matrix2f(5f, 0f, 0f, 3f).rotate(PIf),
            1e-6
        )
        assertEquals(
            Matrix2f(0f, -3f, 5f, 0f),
            Matrix2f(5f, 0f, 0f, 3f).rotate(-PIf * 0.5f),
            1e-6
        )
    }

    @Test
    fun testAdd() {
        assertEquals(
            Matrix2f(2f, 5f, 8f, 11f),
            Matrix2f(1f, 2f, 3f, 4f).add(Matrix2f(1f, 3f, 5f, 7f))
        )
    }

    @Test
    fun testSub() {
        assertEquals(
            Matrix2f(0f, -1f, -2f, -3f),
            Matrix2f(1f, 2f, 3f, 4f).sub(Matrix2f(1f, 3f, 5f, 7f))
        )
    }

    @Test
    fun testMulComponentWise() {
        assertEquals(
            Matrix2f(1f, 6f, 15f, 28f),
            Matrix2f(1f, 2f, 3f, 4f).mulComponentWise(Matrix2f(1f, 3f, 5f, 7f))
        )
    }

    @Test
    fun testLerp() {
        assertEquals(
            Matrix2f(1f, 7f / 3f, 11 / 3f, 5f),
            Matrix2f(1f, 2f, 3f, 4f).lerp(Matrix2f(1f, 3f, 5f, 7f), 1f / 3f),
            1e-6
        )
    }

    @Test
    fun testIsFinite() {
        assertTrue(Matrix2f(0f, 1f, 2f, 3f).isFinite)
        assertFalse(Matrix2f(Float.NaN, 0f, 0f, 0f).isFinite)
        assertFalse(Matrix2f(0f, Float.POSITIVE_INFINITY, 0f, 0f).isFinite)
        assertFalse(Matrix2f(0f, 0f, Float.NEGATIVE_INFINITY, 0f).isFinite)
        assertFalse(Matrix2f(0f, 0f, 0f, Float.NaN).isFinite)
    }

    @Test
    fun testGetScale() {
        assertEquals(Vector2f(3f, 5f), Matrix2f().scale(3f, 5f).getScale(Vector2f()))
    }

    @Test
    fun testGetRotation() {
        val angle = 1f
        assertEquals(angle, Matrix2f().rotate(angle).rotation)
    }

    @Test
    fun testGetDeterminant() {
        assertEquals(1f, Matrix2f().determinant())
        assertEquals(1f, Matrix2f().rotate(1f).determinant(), 1e-7f)
    }

    @Test
    fun testRotation() {
        assertEquals(Matrix2f().rotation(2f), Matrix2f().rotate(1f).rotate(1f), 1e-6)
    }

    @Test
    fun testInvert() {
        assertEquals(
            Matrix2f(), Matrix2f(1f, 2f, 3f, 4f)
                .invert().mul(Matrix2f(1f, 2f, 3f, 4f))
        )
        assertEquals(
            Matrix2f(), Matrix2f(1f, 2f, 3f, 4f)
                .invert().mulLocal(Matrix2f(1f, 2f, 3f, 4f))
        )
    }
}