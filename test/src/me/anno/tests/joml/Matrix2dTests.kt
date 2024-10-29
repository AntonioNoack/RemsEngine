package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.joml.Matrix2d
import org.joml.Matrix3d
import org.joml.Vector2d
import org.junit.jupiter.api.Test
import kotlin.math.PI

class Matrix2dTests {
    @Test
    fun testConstructors() {
        assertEquals(
            Matrix2d(1.0, 2.0, 3.0, 4.0),
            Matrix2d(Matrix2d(1.0, 2.0, 3.0, 4.0))
        )
        assertEquals(
            Matrix2d(1.0, 2.0, 3.0, 4.0),
            Matrix2d(Matrix3d(1.0, 2.0, 0.0, 3.0, 4.0, 0.0, 0.0, 0.0, 1.0))
        )
        assertEquals(
            Matrix2d(1.0, 2.0, 3.0, 4.0),
            Matrix2d(Vector2d(1.0, 2.0), Vector2d(3.0, 4.0))
        )
        assertEquals(
            Matrix2d(),
            Matrix2d(1.0, 0.0, 0.0, 1.0)
        )
    }

    @Test
    fun testScaling() {
        assertEquals(
            Matrix2d(3.0, 0.0, 0.0, 5.0),
            Matrix2d().scaling(3.0, 5.0)
        )
    }

    @Test
    fun testScale() {
        assertEquals(
            Matrix2d(0.0, 3.0, -5.0, 0.0),
            Matrix2d().rotate(PI * 0.5).scale(3.0, 5.0),
            1e-15
        )
    }

    @Test
    fun testScaleLocal() {
        assertEquals(
            Matrix2d(0.0, 5.0, -3.0, 0.0),
            Matrix2d().rotate(PI * 0.5).scaleLocal(3.0, 5.0),
            1e-15
        )
    }

    @Test
    fun testRotate() {
        assertEquals(
            Matrix2d(0.0, 3.0, -5.0, 0.0),
            Matrix2d(5.0, 0.0, 0.0, 3.0).rotate(PI * 0.5),
            1e-15
        )
        assertEquals(
            Matrix2d(-5.0, 0.0, 0.0, -3.0),
            Matrix2d(5.0, 0.0, 0.0, 3.0).rotate(PI),
            1e-15
        )
        assertEquals(
            Matrix2d(0.0, -3.0, 5.0, 0.0),
            Matrix2d(5.0, 0.0, 0.0, 3.0).rotate(-PI * 0.5),
            1e-15
        )
    }

    @Test
    fun testAdd() {
        assertEquals(
            Matrix2d(2.0, 5.0, 8.0, 11.0),
            Matrix2d(1.0, 2.0, 3.0, 4.0).add(Matrix2d(1.0, 3.0, 5.0, 7.0))
        )
    }

    @Test
    fun testSub() {
        assertEquals(
            Matrix2d(0.0, -1.0, -2.0, -3.0),
            Matrix2d(1.0, 2.0, 3.0, 4.0).sub(Matrix2d(1.0, 3.0, 5.0, 7.0))
        )
    }

    @Test
    fun testMulComponentWise() {
        assertEquals(
            Matrix2d(1.0, 6.0, 15.0, 28.0),
            Matrix2d(1.0, 2.0, 3.0, 4.0).mulComponentWise(Matrix2d(1.0, 3.0, 5.0, 7.0))
        )
    }

    @Test
    fun testLerp() {
        assertEquals(
            Matrix2d(1.0, 7.0 / 3.0, 11.0 / 3.0, 5.0),
            Matrix2d(1.0, 2.0, 3.0, 4.0).lerp(Matrix2d(1.0, 3.0, 5.0, 7.0), 1.0 / 3.0),
            1e-16
        )
    }

    @Test
    fun testIsFinite() {
        assertTrue(Matrix2d(0.0, 1.0, 2.0, 3.0).isFinite)
        assertFalse(Matrix2d(Double.NaN, 0.0, 0.0, 0.0).isFinite)
        assertFalse(Matrix2d(0.0, Double.POSITIVE_INFINITY, 0.0, 0.0).isFinite)
        assertFalse(Matrix2d(0.0, 0.0, Double.NEGATIVE_INFINITY, 0.0).isFinite)
        assertFalse(Matrix2d(0.0, 0.0, 0.0, Double.NaN).isFinite)
    }

    @Test
    fun testGetScale() {
        assertEquals(Vector2d(3.0, 5.0), Matrix2d().scale(3.0, 5.0).getScale(Vector2d()))
    }

    @Test
    fun testGetRotation() {
        val angle = 1.0
        assertEquals(angle, Matrix2d().rotate(angle).rotation)
    }

    @Test
    fun testGetDeterminant() {
        assertEquals(1.0, Matrix2d().determinant())
        assertEquals(1.0, Matrix2d().rotate(1.0).determinant(), 1e-16)
    }

    @Test
    fun testRotation() {
        assertEquals(Matrix2d().rotation(2.0), Matrix2d().rotate(1.0).rotate(1.0), 1e-15)
    }

    @Test
    fun testInvert() {
        assertEquals(
            Matrix2d(), Matrix2d(1.0, 2.0, 3.0, 4.0)
                .invert().mul(Matrix2d(1.0, 2.0, 3.0, 4.0))
        )
        assertEquals(
            Matrix2d(), Matrix2d(1.0, 2.0, 3.0, 4.0)
                .invert().mulLocal(Matrix2d(1.0, 2.0, 3.0, 4.0))
        )
    }
}