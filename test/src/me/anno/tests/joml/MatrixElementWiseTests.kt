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
import org.junit.jupiter.api.Test
import kotlin.random.Random

class MatrixElementWiseTests {

    fun <M : Matrix<*, *, *>> testElementWise(
        createMatrix: () -> M,
        transformMatrix: (M, M) -> M,
        transformScalar: (Double, Double) -> Double,
        threshold: Double = 0.0
    ) {
        val rnd = Random(13245)
        val a = createMatrix()
        val b = createMatrix()
        val c = createMatrix()
        for (i in 0 until 20) {
            for (col in 0 until a.numCols) {
                for (row in 0 until a.numRows) {
                    a[col, row] = rnd.nextDouble()
                    b[col, row] = rnd.nextDouble()
                    c[col, row] = transformScalar(a[col, row], b[col, row])
                }
            }
            assertEquals(c, transformMatrix(a, b), threshold)
        }
    }

    @Test
    fun testAddition() {
        val add = { a: Double, b: Double -> a + b }
        testElementWise(::Matrix2f, Matrix2f::add, add)
        testElementWise(::Matrix2d, Matrix2d::add, add)
        testElementWise(::Matrix3x2f, Matrix3x2f::add, add)
        testElementWise(::Matrix3x2d, Matrix3x2d::add, add)
        testElementWise(::Matrix3f, Matrix3f::add, add)
        testElementWise(::Matrix3d, Matrix3d::add, add)
        testElementWise(::Matrix4x3, Matrix4x3::add, add)
        testElementWise(::Matrix4x3f, Matrix4x3f::add, add)
        testElementWise(::Matrix4x3d, Matrix4x3d::add, add)
        testElementWise(::Matrix4f, Matrix4f::add, add)
        testElementWise(::Matrix4d, Matrix4d::add, add)
    }

    @Test
    fun testSubtraction() {
        val sub = { a: Double, b: Double -> a - b }
        testElementWise(::Matrix2f, Matrix2f::sub, sub)
        testElementWise(::Matrix2d, Matrix2d::sub, sub)
        testElementWise(::Matrix3x2f, Matrix3x2f::sub, sub)
        testElementWise(::Matrix3x2d, Matrix3x2d::sub, sub)
        testElementWise(::Matrix3f, Matrix3f::sub, sub)
        testElementWise(::Matrix3d, Matrix3d::sub, sub)
        testElementWise(::Matrix4x3, Matrix4x3::sub, sub)
        testElementWise(::Matrix4x3f, Matrix4x3f::sub, sub)
        testElementWise(::Matrix4x3d, Matrix4x3d::sub, sub)
        testElementWise(::Matrix4f, Matrix4f::sub, sub)
        testElementWise(::Matrix4d, Matrix4d::sub, sub)
    }

    @Test
    fun testMixing() {
        val tf = 2e-7
        val td = 1e-16
        for (d in listOf(0.1, 0.7, 0.9)) {
            val f = d.toFloat()
            val mix = { a: Double, b: Double -> a + (b - a) * d }
            testElementWise(::Matrix2f, { a, b -> a.lerp(b, f) }, mix, tf)
            testElementWise(::Matrix2d, { a, b -> a.lerp(b, d) }, mix, td)
            testElementWise(::Matrix3x2f, { a, b -> a.lerp(b, f) }, mix, tf)
            testElementWise(::Matrix3x2d, { a, b -> a.lerp(b, d) }, mix, td)
            testElementWise(::Matrix3f, { a, b -> a.lerp(b, f) }, mix, tf)
            testElementWise(::Matrix3d, { a, b -> a.lerp(b, d) }, mix, td)
            testElementWise(::Matrix4x3, { a, b -> a.lerp(b, f) }, mix, tf)
            testElementWise(::Matrix4x3f, { a, b -> a.lerp(b, f) }, mix, tf)
            testElementWise(::Matrix4x3d, { a, b -> a.lerp(b, d) }, mix, td)
            testElementWise(::Matrix4f, { a, b -> a.lerp(b, f) }, mix, tf)
            testElementWise(::Matrix4d, { a, b -> a.lerp(b, d) }, mix, td)
        }
    }

    @Test
    fun testMultiplication() {
        val mul = { a: Double, b: Double -> a * b }
        testElementWise(::Matrix2f, Matrix2f::mulComponentWise, mul)
        testElementWise(::Matrix2d, Matrix2d::mulComponentWise, mul)
        testElementWise(::Matrix3x2f, Matrix3x2f::mulComponentWise, mul)
        testElementWise(::Matrix3x2d, Matrix3x2d::mulComponentWise, mul)
        testElementWise(::Matrix3f, Matrix3f::mulComponentWise, mul)
        testElementWise(::Matrix3d, Matrix3d::mulComponentWise, mul)
        testElementWise(::Matrix4x3, Matrix4x3::mulComponentWise, mul)
        testElementWise(::Matrix4x3f, Matrix4x3f::mulComponentWise, mul)
        testElementWise(::Matrix4x3d, Matrix4x3d::mulComponentWise, mul)
        testElementWise(::Matrix4f, Matrix4f::mulComponentWise, mul)
        testElementWise(::Matrix4d, Matrix4d::mulComponentWise, mul)
    }

    @Test
    fun testFma() {
        val fma = { a: Double, b: Double -> a + b * 0.3 }
        testElementWise(::Matrix4x3, { a, b -> a.fma(b, 0.3f) }, fma, 3e-7)
        testElementWise(::Matrix4x3f, { a, b -> a.fma(b, 0.3f) }, fma, 3e-7)
        testElementWise(::Matrix4x3d, { a, b -> a.fma(b, 0.3) }, fma)
    }

    @Test
    fun testAdd4x3() {
        val add = { a: Double, b: Double -> a + b }
        testElementWise(::Matrix4x3f, { a, b -> a.set(Matrix4f(a).add4x3(Matrix4f(b))) }, add)
        testElementWise(::Matrix4x3d, { a, b -> a.set(Matrix4d(a).add4x3(Matrix4d(b))) }, add)
    }

    @Test
    fun testSub4x3() {
        val sub = { a: Double, b: Double -> a - b }
        testElementWise(::Matrix4x3f, { a, b -> a.set(Matrix4f(a).sub4x3(Matrix4f(b))) }, sub)
        testElementWise(::Matrix4x3d, { a, b -> a.set(Matrix4d(a).sub4x3(Matrix4d(b))) }, sub)
    }

    @Test
    fun testMul4x3() {
        val mul = { a: Double, b: Double -> a * b }
        testElementWise(::Matrix4x3f, { a, b -> a.set(Matrix4f(a).mul4x3ComponentWise(Matrix4f(b))) }, mul)
        testElementWise(::Matrix4x3d, { a, b -> a.set(Matrix4d(a).mul4x3ComponentWise(Matrix4d(b))) }, mul)
    }

    @Test
    fun testFma4x3() {
        val fma = { a: Double, b: Double -> a + b * 0.3 }
        testElementWise(::Matrix4x3f, { a, b -> a.set(Matrix4f(a).fma4x3(Matrix4f(b), 0.3f)) }, fma, 3e-7)
        testElementWise(::Matrix4x3d, { a, b -> a.set(Matrix4d(a).fma4x3(Matrix4d(b), 0.3)) }, fma)
    }
}