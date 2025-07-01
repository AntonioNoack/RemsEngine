package me.anno.tests.joml

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.random.Random

class Vector3dTests {
    @Test
    fun testConstructors() {
        assertEquals(Vector3d(1.0, 1.0, 1.0), Vector3d(1.0))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(1.0, 2.0, 3.0))
        assertEquals(Vector3d(0.0, 0.0, 0.0), Vector3d())
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(Vector3i(1, 2, 3)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(Vector3f(1, 2, 3)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(Vector3d(1.0, 2.0, 3.0)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(doubleArrayOf(1.0, 2.0, 3.0)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(doubleArrayOf(5.0, 1.0, 2.0, 3.0), 1))
    }

    @Test
    fun testSetters() {
        assertEquals(Vector3d(0.0, 0.0, 0.0), Vector3d())
        assertEquals(Vector3d(1.0, 1.0, 1.0), Vector3d().set(1.0))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d().set(1.0, 2.0, 3.0))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d().set(Vector3i(1, 2, 3)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d().set(Vector3f(1f, 2f, 3f)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d().set(Vector3d(1.0, 2.0, 3.0)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d().set(doubleArrayOf(1.0, 2.0, 3.0)))
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d().set(doubleArrayOf(5.0, 1.0, 2.0, 3.0), 1))
    }

    @Test
    fun testComponents() {
        val (x, y, z) = Vector3d(1.0, 2.0, 3.0)
        assertEquals(1.0, x)
        assertEquals(2.0, y)
        assertEquals(3.0, z)
    }

    @Test
    fun testLengths() {
        assertEquals(sqrt(3.0 * 3.0 + 2.0 * 2.0 + 4.0 * 4.0), Vector3d(3.0, 2.0, 4.0).length())
        assertEquals(3.0 * 3.0 + 2.0 * 2.0 + 4.0 * 4.0, Vector3d(3.0, 2.0, 4.0).lengthSquared())
    }

    @Test
    fun testDistances() {
        assertEquals(sqrt(2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0), Vector3d(3f, 2f, 4f).distance(5.0, 3.0, 0.0))
        assertEquals(sqrt(2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0), Vector3d(3f, 2f, 4f).distance(Vector3d(5.0, 3.0, 0.0)))
        assertEquals(2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0, Vector3d(3f, 2f, 4f).distanceSquared(5.0, 3.0, 0.0))
        assertEquals(2.0 * 2.0 + 1.0 * 1.0 + 4.0 * 4.0, Vector3d(3f, 2f, 4f).distanceSquared(Vector3d(5.0, 3.0, 0.0)))
    }

    @Test
    fun testNormalize() {
        val a = Vector3d(3.0, 5.0, 4.0)
        val b = a.normalize(Vector3d())
        assertNotEquals(a, b)
        assertEquals(1.0, b.length(), 1e-6)
        assertEquals(0.0, a.angle(b), 1e-3)
    }

    @Test
    fun testNormalize2() {
        val a = Vector3d(3.0, 4.0, 5.0)
        val b = a.normalize(2.0, Vector3d())
        assertNotEquals(a, b)
        assertEquals(2.0, b.length(), 1e-6)
        assertEquals(0.0, a.angle(b))
    }

    @Test
    fun testSafeNormalize() {
        val a = Vector3d(3.0, 5.0, 4.0)
        val b = Vector3d(a).safeNormalize()
        assertNotEquals(a, b)
        assertEquals(1.0, b.length(), 1e-6)
        assertEquals(0.0, a.angle(b), 1e-3)
        assertEquals(Vector3d(), Vector3d().safeNormalize())
    }

    @Test
    fun testFma() {
        assertEquals(Vector3d(3f, 5f, 7f), Vector3d(1.0).fma(2.0, Vector3d(1.0, 2.0, 3.0)))
        assertEquals(Vector3d(3f, 7f, 17f), Vector3d(1.0).fma(Vector3d(2f, 3f, 4f), Vector3d(1f, 2f, 4f)))
    }

    @Test
    fun testMinMaxComponent() {
        assertEquals(0, Vector3d(1f, 2f, 2f).minComponent())
        assertEquals(1, Vector3d(2f, 1f, 2f).minComponent())
        assertEquals(2, Vector3d(2f, 2f, 1f).minComponent())
        assertEquals(0, Vector3d(2f, 1f, 1f).maxComponent())
        assertEquals(1, Vector3d(1f, 2f, 1f).maxComponent())
        assertEquals(2, Vector3d(1f, 1f, 2f).maxComponent())
    }

    @Test
    fun testMinMax() {
        assertEquals(1.0, Vector3d(1.0, 2.0, 2.0).min())
        assertEquals(1.0, Vector3d(2.0, 1.0, 2.0).min())
        assertEquals(1.0, Vector3d(2.0, 2.0, 1.0).min())
        assertEquals(2.0, Vector3d(2.0, 1.0, 1.0).max())
        assertEquals(2.0, Vector3d(1.0, 2.0, 1.0).max())
        assertEquals(2.0, Vector3d(1.0, 1.0, 2.0).max())
    }

    @Test
    fun testOrthogonalize() {
        val rnd = Random(123)
        val vec = Vector3d()
        val ortho = Vector3d()
        repeat(100) {
            vec.set(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble())
                .sub(0.5)
            vec.orthogonalize(ortho)
            assertEquals(0.0, vec.dot(ortho), 1e-16)
            assertEquals(1.0, ortho.lengthSquared(), 1e-15)
        }
    }

    @Test
    fun testFloorRoundCeil() {
        assertEquals(Vector3d(1f, 2f, 1f), Vector3d(1.3f, 2.9f, 1.7f).floor())
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(1.3f, 1.9f, 2.6f).round())
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(0.3f, 1.9f, 2.1f).ceil())
    }

    @Test
    fun testIsFinite() {
        assertTrue(Vector3d(1e38).isFinite)
        assertFalse(Vector3d(0f, Float.NaN, 0f).isFinite)
        assertFalse(Vector3d(Float.POSITIVE_INFINITY, 1f, 5f).isFinite)
        assertFalse(Vector3d(1f, -5f, Float.NEGATIVE_INFINITY).isFinite)
    }

    @Test
    fun testAbsolute() {
        assertEquals(Vector3d(1.0, 2.0, 3.0), Vector3d(-1.0, 2.0, -3.0).absolute())
    }

    @Test
    fun testCross() {
        assertEquals(Vector3d(-1.0, 2.0, -1.0), Vector3d(1.0, 2.0, 3.0).cross(3.0, 5.0, 7.0))
        assertEquals(Vector3d(-1.0, 2.0, -1.0), Vector3d(1.0, 2.0, 3.0).cross(Vector3d(3.0, 5.0, 7.0)))
    }

    @Test
    fun testMulAdd() {
        assertEquals(
            Vector3d(8.0, 13.0, 18.0),
            Vector3d(1.0, 2.0, 3.0).mulAdd(3.0, Vector3d(5.0, 7.0, 9.0), Vector3d())
        )
    }
}