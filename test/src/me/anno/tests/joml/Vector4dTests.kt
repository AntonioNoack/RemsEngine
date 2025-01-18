package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random

class Vector4dTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector4d(0.0, 0.0, 0.0, 1.0), Vector4d())
        assertEquals(Vector4d(2.0, 2.0, 2.0, 2.0), Vector4d(2.0))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(Vector4f(1f, 2f, 3f, 4f)))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(Vector4i(1, 2, 3, 4)))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(Vector3d(1.0, 2.0, 3.0), 4.0))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(floatArrayOf(0f, 1f, 2f, 3f, 4f), 1))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector4d(2.0, 2.0, 2.0, 2.0), Vector4d().set(2.0))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(1f, 2f, 3f, 4f))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(1.0, 2.0, 3.0, 4.0))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(Vector4i(1, 2, 3, 4)))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 1.0), Vector4d().set(1.0, 2.0, 3.0))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(Vector3d(1.0, 2.0, 3.0), 4.0))
        assertEquals(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d().set(doubleArrayOf(0.0, 1.0, 2.0, 3.0, 4.0), 1))
    }

    @Test
    fun testComponents() {
        val (x, y, z, w) = Vector4d(1.0, 2.0, 3.0, 4.0)
        assertEquals(1.0, x)
        assertEquals(2.0, y)
        assertEquals(3.0, z)
        assertEquals(4.0, w)
    }

    @Test
    fun testLengths() {
        val expected = 3.0 * 3.0 + 2.0 * 2.0 + 4.0 * 4.0 + 1.0 * 1.0
        assertEquals(sqrt(expected), Vector4d(3.0, 2.0, 4.0, -1.0).length())
        assertEquals(expected, Vector4d(3.0, 2.0, 4.0, -1.0).lengthSquared())
        val expected3 = 3.0 * 3.0 + 2.0 * 2.0 + 4.0 * 4.0
        assertEquals(sqrt(expected3), Vector4d(3.0, 2.0, 4.0, -1.0).length3())
    }

    @Test
    fun testDistances() {
        val expected = 2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0 + 1.0 * 1.0
        assertEquals(sqrt(expected), Vector4d(3.0, 2.0, 4.0, 1.0).distance(5.0, 3.0, 0.0, 2.0))
        assertEquals(sqrt(expected), Vector4d(3.0, 2.0, 4.0, 1.0).distance(Vector4d(5.0, 3.0, 0.0, 2.0)))
        assertEquals(expected, Vector4d(3.0, 2.0, 4.0, 1.0).distanceSquared(5.0, 3.0, 0.0, 2.0))
        assertEquals(expected, Vector4d(3.0, 2.0, 4.0, 1.0).distanceSquared(Vector4d(5.0, 3.0, 0.0, 2.0)))
    }

    @Test
    fun testNormalize() {
        val a = Vector4d(3.0, 5.0, 4.0, 1.0)
        val b = a.normalize(Vector4d())
        assertNotEquals(a, b)
        assertEquals(1.0, b.length(), 1e-6)
        assertEquals(0.0, a.angle(b), 1e-3)
    }

    @Test
    fun testNormalize2() {
        val a = Vector4d(3.0, 4.0, 5.0, 1.0)
        val b = a.normalize(2.0, Vector4d())
        assertNotEquals(a, b)
        assertEquals(2.0, b.length(), 1e-6)
        assertEquals(0.0, a.angle(b))
    }

    @Test
    fun testSafeNormalize() {
        val a = Vector4d(3.0, 5.0, 4.0, 1.0)
        val b = Vector4d(a).safeNormalize()
        assertNotEquals(a, b)
        assertEquals(1.0, b.length(), 1e-6)
        assertEquals(0.0, a.angle(b), 1e-3)
        assertEquals(Vector4d(), Vector4d().safeNormalize())
    }

    @Test
    fun testFma() {
        assertEquals(Vector4d(3.0, 5.0, 7.0, 9.0), Vector4d(1.0).fma(2.0, Vector4d(1.0, 2.0, 3.0, 4.0)))
        assertEquals(
            Vector4d(3.0, 7.0, 17.0, 36.0),
            Vector4d(1.0).fma(
                Vector4d(2.0, 3.0, 4.0, 5.0),
                Vector4d(1.0, 2.0, 4.0, 7.0)
            )
        )
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector4d(1.0, 2.0, 2.0, 2.0).minComponent())
        assertEquals(1, Vector4d(2.0, 1.0, 2.0, 2.0).minComponent())
        assertEquals(2, Vector4d(2.0, 2.0, 1.0, 2.0).minComponent())
        assertEquals(3, Vector4d(2.0, 2.0, 2.0, 1.0).minComponent())
        assertEquals(0, Vector4d(2.0, 1.0, 1.0, 1.0).maxComponent())
        assertEquals(1, Vector4d(1.0, 2.0, 1.0, 1.0).maxComponent())
        assertEquals(2, Vector4d(1.0, 1.0, 2.0, 1.0).maxComponent())
        assertEquals(3, Vector4d(1.0, 1.0, 1.0, 2.0).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1.0, Vector4d(1.0, 2.0, 2.0, 2.0).min())
        assertEquals(1.0, Vector4d(2.0, 1.0, 2.0, 2.0).min())
        assertEquals(1.0, Vector4d(2.0, 2.0, 1.0, 2.0).min())
        assertEquals(1.0, Vector4d(2.0, 2.0, 2.0, 1.0).min())
        assertEquals(2.0, Vector4d(2.0, 1.0, 1.0, 1.0).max())
        assertEquals(2.0, Vector4d(1.0, 2.0, 1.0, 1.0).max())
        assertEquals(2.0, Vector4d(1.0, 1.0, 2.0, 1.0).max())
        assertEquals(2.0, Vector4d(1.0, 1.0, 1.0, 2.0).max())
    }

    @Test
    fun testFloorRoundCeil() {
        assertEquals(Vector4d(1.0, 2.0, 1.0, -1.0), Vector4d(1.3, 2.9, 1.7, -0.2).floor())
        assertEquals(Vector4d(1.0, 2.0, 3.0, 0.0), Vector4d(1.3, 1.9, 2.6, -0.1).round())
        assertEquals(Vector4d(1.0, 2.0, 3.0, 0.0), Vector4d(0.3, 1.9, 2.1, -0.9).ceil())
    }

    @Test
    fun testIsFinite() {
        assertTrue(Vector4d(1e38).isFinite)
        assertFalse(Vector4d(0.0, Double.NaN, 0.0, 5.0).isFinite)
        assertFalse(Vector4d(Double.POSITIVE_INFINITY, 1.0, 5.0, 1.0).isFinite)
        assertFalse(Vector4d(1.0, -5.0, Double.NEGATIVE_INFINITY, 7.0).isFinite)
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector4d(1.0, 2.0, 3.0, 3.0), Vector4d(-1.0, 2.0, -3.0, 3.0).absolute())
    }

    @Test
    fun testMulAdd() {
        assertEquals(
            Vector4d(8.0, 13.0, 18.0, 1.0),
            Vector4d(1.0, 2.0, 3.0, 4.0).mulAdd3(3.0, Vector4d(5.0, 7.0, 9.0, 11.0), Vector4d())
        )
        assertEquals(
            Vector4d(8.0, 13.0, 18.0, 23.0),
            Vector4d(1.0, 2.0, 3.0, 4.0).mulAdd(3.0, Vector4d(5.0, 7.0, 9.0, 11.0), Vector4d())
        )
    }

    @Test
    fun testMulMat4f() {
        val mat4d = Matrix4d()
        val row = Vector4d()
        val random = Random(1234)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                row[j] = random.nextDouble() * 2.0 - 1.0
            }
            mat4d.setRow(i, row)
        }
        val mat4f = Matrix4f()
        val vector = Vector4d()
        for (j in 0 until 4) {
            vector[j] = random.nextDouble()
        }
        val baselineVec = vector.mul(mat4d, Vector4d())
        assertEquals(baselineVec, vector.mul(mat4f.set(mat4d), Vector4d()), 1e-7)
    }

    @Test
    fun testMulProject() {
        val mat4d = Matrix4d()
        val row = Vector4d()
        val random = Random(1234)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                row[j] = random.nextDouble() * 2.0 - 1.0
            }
            mat4d.setRow(i, row)
        }
        val vec3 = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
        val vec4 = Vector4d(vec3, 1.0)
        val baselineVec = vec4.mulProject(mat4d, Vector4d())
        assertEquals(baselineVec, Vector4d(vec3.mulProject(mat4d, Vector3d()), 1.0), 1e-7)
    }
}