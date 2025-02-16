package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import org.joml.Matrix
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MatrixGetSetTests {

    fun <M : Matrix<*, *, *>> M.fill(i0: Double = 1.0): M {
        for (i in 0 until numCols * numRows) {
            this[i / numRows, i % numRows] = i + i0
        }
        return this
    }

    @Test
    fun testFill() {
        assertEquals(Matrix3x2f(1f, 2f, 3f, 4f, 5f, 6f), Matrix3x2f().fill())
    }

    @Test
    fun testSetGet() {
        testSetGet(Matrix2f(), 1e-7)
        testSetGet(Matrix2d(), 0.0)
        testSetGet(Matrix3x2f(), 1e-7)
        testSetGet(Matrix3x2d(), 0.0)
        testSetGet(Matrix3f(), 1e-7)
        testSetGet(Matrix3d(), 0.0)
        testSetGet(Matrix4x3(), 1e-7)
        testSetGet(Matrix4x3f(), 1e-7)
        testSetGet(Matrix4x3d(), 0.0)
        testSetGet(Matrix4f(), 1e-7)
        testSetGet(Matrix4d(), 0.0)
    }

    fun testSetGet(m: Matrix<*, *, *>, threshold: Double) {
        val numbers = DoubleArray(m.numRows * m.numCols)
        var i = 0
        val random = Random(1234)
        for (col in 0 until m.numCols) {
            for (row in 0 until m.numRows) {
                val number = random.nextDouble()
                m[col, row] = number
                numbers[i++] = number
            }
        }
        i = 0
        for (col in 0 until m.numCols) {
            for (row in 0 until m.numRows) {
                assertEquals(numbers[i++], m[col, row], threshold)
            }
        }
    }

    @Test
    fun testGetColumn2f() {
        val m = Matrix2f().fill()
        assertEquals(Vector2f(1f, 2f), m.getColumn(0, Vector2f()))
        assertEquals(Vector2f(3f, 4f), m.getColumn(1, Vector2f()))
    }

    @Test
    fun testSetColumn2f() {
        val m = Matrix2f()
        m.setColumn(0, Vector2f(1f, 2f))
        m.setColumn(1, Vector2f(3f, 4f))
        assertEquals(Matrix2f().fill(), m)
    }

    @Test
    fun testGetColumn2d() {
        val m = Matrix2d().fill()
        assertEquals(Vector2d(1f, 2f), m.getColumn(0, Vector2d()))
        assertEquals(Vector2d(3f, 4f), m.getColumn(1, Vector2d()))
    }

    @Test
    fun testSetColumn2d() {
        val m = Matrix2d()
        m.setColumn(0, Vector2d(1f, 2f))
        m.setColumn(1, Vector2d(3f, 4f))
        assertEquals(Matrix2d().fill(), m)
    }

    @Test
    fun testGetColumn3x2f() {
        val m = Matrix3x2f().fill()
        assertEquals(Vector2f(1f, 2f), m.getColumn(0, Vector2f()))
        assertEquals(Vector2f(3f, 4f), m.getColumn(1, Vector2f()))
        assertEquals(Vector2f(5f, 6f), m.getColumn(2, Vector2f()))
    }

    @Test
    fun testGetColumn3x2d() {
        val m = Matrix3x2d().fill()
        assertEquals(Vector2d(1f, 2f), m.getColumn(0, Vector2d()))
        assertEquals(Vector2d(3f, 4f), m.getColumn(1, Vector2d()))
        assertEquals(Vector2d(5f, 6f), m.getColumn(2, Vector2d()))
    }

    @Test
    fun testSetColumn3x2d() {
        val m = Matrix3x2d()
        m.setColumn(0, Vector2d(1f, 2f))
        m.setColumn(1, Vector2d(3f, 4f))
        m.setColumn(2, Vector2d(5f, 6f))
        assertEquals(Matrix3x2d().fill(), m)
    }

    @Test
    fun testGetColumn3f() {
        val m = Matrix3f().fill()
        assertEquals(Vector3f(1f, 2f, 3f), m.getColumn(0, Vector3f()))
        assertEquals(Vector3f(4f, 5f, 6f), m.getColumn(1, Vector3f()))
        assertEquals(Vector3f(7f, 8f, 9f), m.getColumn(2, Vector3f()))
    }

    @Test
    fun testSetColumn3f() {
        val m = Matrix3f()
        m.setColumn(0, Vector3f(1f, 2f, 3f))
        m.setColumn(1, Vector3f(4f, 5f, 6f))
        m.setColumn(2, Vector3f(7f, 8f, 9f))
        assertEquals(Matrix3f().fill(), m)
    }

    @Test
    fun testGetColumn3d() {
        val m = Matrix3d().fill()
        assertEquals(Vector3d(1f, 2f, 3f), m.getColumn(0, Vector3d()))
        assertEquals(Vector3d(4f, 5f, 6f), m.getColumn(1, Vector3d()))
        assertEquals(Vector3d(7f, 8f, 9f), m.getColumn(2, Vector3d()))
    }

    @Test
    fun testSetColumn3d() {
        val m = Matrix3d()
        m.setColumn(0, Vector3d(1f, 2f, 3f))
        m.setColumn(1, Vector3d(4f, 5f, 6f))
        m.setColumn(2, Vector3d(7f, 8f, 9f))
        assertEquals(Matrix3d().fill(), m)
    }

    @Test
    fun testGetColumn4x3f() {
        val m = Matrix4x3f().fill()
        assertEquals(Vector3f(1f, 2f, 3f), m.getColumn(0, Vector3f()))
        assertEquals(Vector3f(4f, 5f, 6f), m.getColumn(1, Vector3f()))
        assertEquals(Vector3f(7f, 8f, 9f), m.getColumn(2, Vector3f()))
        assertEquals(Vector3f(10f, 11f, 12f), m.getColumn(3, Vector3f()))
    }

    @Test
    fun testSetColumn4x3f() {
        val m = Matrix4x3f()
        m.setColumn(0, Vector3f(1f, 2f, 3f))
        m.setColumn(1, Vector3f(4f, 5f, 6f))
        m.setColumn(2, Vector3f(7f, 8f, 9f))
        m.setColumn(3, Vector3f(10f, 11f, 12f))
        assertEquals(Matrix4x3f().fill(), m)
    }

    @Test
    fun testGetColumn4x3() {
        val m = Matrix4x3().fill()
        assertEquals(Vector3d(1f, 2f, 3f), m.getColumn(0, Vector3d()))
        assertEquals(Vector3d(4f, 5f, 6f), m.getColumn(1, Vector3d()))
        assertEquals(Vector3d(7f, 8f, 9f), m.getColumn(2, Vector3d()))
        assertEquals(Vector3d(10f, 11f, 12f), m.getColumn(3, Vector3d()))
    }

    @Test
    fun testSetColumn4x3() {
        val m = Matrix4x3()
        m.setColumn(0, Vector3d(1f, 2f, 3f))
        m.setColumn(1, Vector3d(4f, 5f, 6f))
        m.setColumn(2, Vector3d(7f, 8f, 9f))
        m.setColumn(3, Vector3d(10f, 11f, 12f))
        assertEquals(Matrix4x3().fill(), m)
    }

    @Test
    fun testGetColumn4x3d() {
        val m = Matrix4x3d().fill()
        assertEquals(Vector3d(1f, 2f, 3f), m.getColumn(0, Vector3d()))
        assertEquals(Vector3d(4f, 5f, 6f), m.getColumn(1, Vector3d()))
        assertEquals(Vector3d(7f, 8f, 9f), m.getColumn(2, Vector3d()))
        assertEquals(Vector3d(10f, 11f, 12f), m.getColumn(3, Vector3d()))
    }

    @Test
    fun testSetColumn4x3d() {
        val m = Matrix4x3d()
        m.setColumn(0, Vector3d(1f, 2f, 3f))
        m.setColumn(1, Vector3d(4f, 5f, 6f))
        m.setColumn(2, Vector3d(7f, 8f, 9f))
        m.setColumn(3, Vector3d(10f, 11f, 12f))
        assertEquals(Matrix4x3d().fill(), m)
    }

    @Test
    fun testGetColumn4f() {
        val m = Matrix4f().fill()
        assertEquals(Vector3f(1f, 2f, 3f), m.getColumn(0, Vector3f()))
        assertEquals(Vector3f(5f, 6f, 7f), m.getColumn(1, Vector3f()))
        assertEquals(Vector3f(9f, 10f, 11f), m.getColumn(2, Vector3f()))
        assertEquals(Vector3f(13f, 14f, 15f), m.getColumn(3, Vector3f()))
        assertEquals(Vector4f(1f, 2f, 3f, 4f), m.getColumn(0, Vector4f()))
        assertEquals(Vector4f(5f, 6f, 7f, 8f), m.getColumn(1, Vector4f()))
        assertEquals(Vector4f(9f, 10f, 11f, 12f), m.getColumn(2, Vector4f()))
        assertEquals(Vector4f(13f, 14f, 15f, 16f), m.getColumn(3, Vector4f()))
    }

    @Test
    fun testSetColumn4f() {
        val m = Matrix4f()
        m.setColumn(0, Vector4f(1f, 2f, 3f, 4f))
        m.setColumn(1, Vector4f(5f, 6f, 7f, 8f))
        m.setColumn(2, Vector4f(9f, 10f, 11f, 12f))
        m.setColumn(3, Vector4f(13f, 14f, 15f, 16f))
        assertEquals(Matrix4f().fill(), m)
    }

    @Test
    fun testGetColumn4d() {
        val m = Matrix4d().fill()
        assertEquals(Vector3d(1f, 2f, 3f), m.getColumn(0, Vector3d()))
        assertEquals(Vector3d(5f, 6f, 7f), m.getColumn(1, Vector3d()))
        assertEquals(Vector3d(9f, 10f, 11f), m.getColumn(2, Vector3d()))
        assertEquals(Vector3d(13f, 14f, 15f), m.getColumn(3, Vector3d()))
        assertEquals(Vector4d(1f, 2f, 3f, 4f), m.getColumn(0, Vector4d()))
        assertEquals(Vector4d(5f, 6f, 7f, 8f), m.getColumn(1, Vector4d()))
        assertEquals(Vector4d(9f, 10f, 11f, 12f), m.getColumn(2, Vector4d()))
        assertEquals(Vector4d(13f, 14f, 15f, 16f), m.getColumn(3, Vector4d()))
    }

    @Test
    fun testSetColumn4d() {
        val m = Matrix4d()
        m.setColumn(0, Vector4d(1f, 2f, 3f, 4f))
        m.setColumn(1, Vector4d(5f, 6f, 7f, 8f))
        m.setColumn(2, Vector4d(9f, 10f, 11f, 12f))
        m.setColumn(3, Vector4d(13f, 14f, 15f, 16f))
        assertEquals(Matrix4d().fill(), m)
    }

    @Test
    fun testGetRow2f() {
        val m = Matrix2f().fill()
        assertEquals(Vector2f(1f, 3f), m.getRow(0, Vector2f()))
        assertEquals(Vector2f(2f, 4f), m.getRow(1, Vector2f()))
    }

    @Test
    fun testSetRow2f() {
        val m = Matrix2f()
        m.setRow(0, Vector2f(1f, 3f))
        m.setRow(1, Vector2f(2f, 4f))
        assertEquals(Matrix2f().fill(), m)
    }

    @Test
    fun testGetRow2d() {
        val m = Matrix2d().fill()
        assertEquals(Vector2d(1f, 3f), m.getRow(0, Vector2d()))
        assertEquals(Vector2d(2f, 4f), m.getRow(1, Vector2d()))
    }

    @Test
    fun testSetRow2d() {
        val m = Matrix2d()
        m.setRow(0, Vector2d(1f, 3f))
        m.setRow(1, Vector2d(2f, 4f))
        assertEquals(Matrix2d().fill(), m)
    }

    @Test
    fun testGetRow3x2f() {
        val m = Matrix3x2f().fill()
        assertEquals(Vector3f(1f, 3f, 5f), m.getRow(0, Vector3f()))
        assertEquals(Vector3f(2f, 4f, 6f), m.getRow(1, Vector3f()))
    }

    @Test
    fun testSetRow3x2f() {
        val m = Matrix3x2f()
        m.setRow(0, Vector3f(1f, 3f, 5f))
        m.setRow(1, Vector3f(2f, 4f, 6f))
        assertEquals(Matrix3x2f().fill(), m)
    }

    @Test
    fun testGetRow3x2d() {
        val m = Matrix3x2d().fill()
        assertEquals(Vector3d(1f, 3f, 5f), m.getRow(0, Vector3d()))
        assertEquals(Vector3d(2f, 4f, 6f), m.getRow(1, Vector3d()))
    }

    @Test
    fun testSetRow3x2d() {
        val m = Matrix3x2d()
        m.setRow(0, Vector3d(1f, 3f, 5f))
        m.setRow(1, Vector3d(2f, 4f, 6f))
        assertEquals(Matrix3x2d().fill(), m)
    }

    @Test
    fun testGetRow3f() {
        val m = Matrix3f().fill()
        assertEquals(Vector3f(1f, 4f, 7f), m.getRow(0, Vector3f()))
        assertEquals(Vector3f(2f, 5f, 8f), m.getRow(1, Vector3f()))
        assertEquals(Vector3f(3f, 6f, 9f), m.getRow(2, Vector3f()))
    }

    @Test
    fun testSetRow3f() {
        val m = Matrix3f()
        m.setRow(0, Vector3f(1f, 4f, 7f))
        m.setRow(1, Vector3f(2f, 5f, 8f))
        m.setRow(2, Vector3f(3f, 6f, 9f))
        assertEquals(Matrix3f().fill(), m)
    }

    @Test
    fun testGetRow3d() {
        val m = Matrix3d().fill()
        assertEquals(Vector3d(1f, 4f, 7f), m.getRow(0, Vector3d()))
        assertEquals(Vector3d(2f, 5f, 8f), m.getRow(1, Vector3d()))
        assertEquals(Vector3d(3f, 6f, 9f), m.getRow(2, Vector3d()))
    }

    @Test
    fun testSetRow3d() {
        val m = Matrix3d()
        m.setRow(0, Vector3d(1f, 4f, 7f))
        m.setRow(1, Vector3d(2f, 5f, 8f))
        m.setRow(2, Vector3d(3f, 6f, 9f))
        assertEquals(Matrix3d().fill(), m)
    }

    @Test
    fun testGetRow4x3f() {
        val m = Matrix4x3f().fill()
        assertEquals(Vector4f(1f, 4f, 7f, 10f), m.getRow(0, Vector4f()))
        assertEquals(Vector4f(2f, 5f, 8f, 11f), m.getRow(1, Vector4f()))
        assertEquals(Vector4f(3f, 6f, 9f, 12f), m.getRow(2, Vector4f()))
    }

    @Test
    fun testSetRow4x3f() {
        val m = Matrix4x3f()
        m.setRow(0, Vector4f(1f, 4f, 7f, 10f))
        m.setRow(1, Vector4f(2f, 5f, 8f, 11f))
        m.setRow(2, Vector4f(3f, 6f, 9f, 12f))
        assertEquals(Matrix4x3f().fill(), m)
    }

    @Test
    fun testGetRow4x3() {
        val m = Matrix4x3().fill()
        assertEquals(Vector4d(1f, 4f, 7f, 10f), m.getRow(0, Vector4d()))
        assertEquals(Vector4d(2f, 5f, 8f, 11f), m.getRow(1, Vector4d()))
        assertEquals(Vector4d(3f, 6f, 9f, 12f), m.getRow(2, Vector4d()))
    }

    @Test
    fun testSetRow4x3() {
        val m = Matrix4x3()
        m.setRow(0, Vector4d(1f, 4f, 7f, 10f))
        m.setRow(1, Vector4d(2f, 5f, 8f, 11f))
        m.setRow(2, Vector4d(3f, 6f, 9f, 12f))
        assertEquals(Matrix4x3().fill(), m)
    }

    @Test
    fun testGetRow4x3d() {
        val m = Matrix4x3d().fill()
        assertEquals(Vector4d(1f, 4f, 7f, 10f), m.getRow(0, Vector4d()))
        assertEquals(Vector4d(2f, 5f, 8f, 11f), m.getRow(1, Vector4d()))
        assertEquals(Vector4d(3f, 6f, 9f, 12f), m.getRow(2, Vector4d()))
    }

    @Test
    fun testSetRow4x3d() {
        val m = Matrix4x3d()
        m.setRow(0, Vector4d(1f, 4f, 7f, 10f))
        m.setRow(1, Vector4d(2f, 5f, 8f, 11f))
        m.setRow(2, Vector4d(3f, 6f, 9f, 12f))
        assertEquals(Matrix4x3d().fill(), m)
    }

    @Test
    fun testGetRow4f() {
        val m = Matrix4f().fill()
        assertEquals(Vector3f(1f, 5f, 9f), m.getRow(0, Vector3f()))
        assertEquals(Vector3f(2f, 6f, 10f), m.getRow(1, Vector3f()))
        assertEquals(Vector3f(3f, 7f, 11f), m.getRow(2, Vector3f()))
        assertEquals(Vector3f(4f, 8f, 12f), m.getRow(3, Vector3f()))
        assertEquals(Vector4f(1f, 5f, 9f, 13f), m.getRow(0, Vector4f()))
        assertEquals(Vector4f(2f, 6f, 10f, 14f), m.getRow(1, Vector4f()))
        assertEquals(Vector4f(3f, 7f, 11f, 15f), m.getRow(2, Vector4f()))
        assertEquals(Vector4f(4f, 8f, 12f, 16f), m.getRow(3, Vector4f()))
    }

    @Test
    fun testSetRow4f() {
        val m = Matrix4f()
        m.setRow(0, Vector4f(1f, 5f, 9f, 13f))
        m.setRow(1, Vector4f(2f, 6f, 10f, 14f))
        m.setRow(2, Vector4f(3f, 7f, 11f, 15f))
        m.setRow(3, Vector4f(4f, 8f, 12f, 16f))
        assertEquals(Matrix4f().fill(), m)
    }

    @Test
    fun testGetRow4d() {
        val m = Matrix4d().fill()
        assertEquals(Vector3d(1f, 5f, 9f), m.getRow(0, Vector3d()))
        assertEquals(Vector3d(2f, 6f, 10f), m.getRow(1, Vector3d()))
        assertEquals(Vector3d(3f, 7f, 11f), m.getRow(2, Vector3d()))
        assertEquals(Vector3d(4f, 8f, 12f), m.getRow(3, Vector3d()))
        assertEquals(Vector4d(1f, 5f, 9f, 13f), m.getRow(0, Vector4d()))
        assertEquals(Vector4d(2f, 6f, 10f, 14f), m.getRow(1, Vector4d()))
        assertEquals(Vector4d(3f, 7f, 11f, 15f), m.getRow(2, Vector4d()))
        assertEquals(Vector4d(4f, 8f, 12f, 16f), m.getRow(3, Vector4d()))
    }

    @Test
    fun testSetRow4d() {
        val m = Matrix4d()
        m.setRow(0, Vector4d(1f, 5f, 9f, 13f))
        m.setRow(1, Vector4d(2f, 6f, 10f, 14f))
        m.setRow(2, Vector4d(3f, 7f, 11f, 15f))
        m.setRow(3, Vector4d(4f, 8f, 12f, 16f))
        assertEquals(Matrix4d().fill(), m)
    }
}