package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.RayIntersection.rayIntersection
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RayIntersectionTests3D {
    @Test
    fun testSampleD() {
        val result = Vector2d()
        assertTrue(
            rayIntersection(
                Vector3d(-1.0, -1.0, 0.0), Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, -1.0), Vector3d(0.0, 0.0, 1.0),
                result
            )
        )
        assertEquals(Vector2d(1.0, 1.0), result)
    }

    @Test
    fun testRandomSamplesD() {
        val rnd = Random(1634)
        repeat(20) {
            val hit = Vector3d()
            val dist1 = (rnd.nextDouble() - 0.5) * 10.0
            val dist2 = (rnd.nextDouble() - 0.5) * 10.0
            val dir1 = Vector3d(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).sub(0.5).normalize()
            val dir2 = Vector3d(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).sub(0.5).normalize()

            val result = Vector2d()
            assertTrue(
                rayIntersection(
                    hit - dir1 * dist1, dir1,
                    hit - dir2 * dist2, dir2,
                    result
                )
            )
            assertEquals(Vector2d(dist1, dist2), result, 1e-13)
        }
    }

    @Test
    fun testParallelRaysD() {
        assertFalse(
            rayIntersection(
                Vector3d(-1.0, -1.0, 0.0), Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, -1.0), Vector3d(1.0, 0.0, 0.0)
            )
        )
    }

    @Test
    fun testSampleF() {
        val result = Vector2f()
        assertTrue(
            rayIntersection(
                Vector3f(-1.0, -1.0, 0.0), Vector3f(1.0, 0.0, 0.0),
                Vector3f(0.0, 1.0, -1.0), Vector3f(0.0, 0.0, 1.0),
                result
            )
        )
        assertEquals(Vector2f(1.0, 1.0), result)
    }

    @Test
    fun testRandomSamplesF() {
        val rnd = Random(1634)
        repeat(20) {
            val hit = Vector3f()
            val dist1 = (rnd.nextFloat() - 0.5f) * 10f
            val dist2 = (rnd.nextFloat() - 0.5f) * 10f
            val dir1 = Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f).normalize()
            val dir2 = Vector3f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f).normalize()

            val result = Vector2f()
            assertTrue(
                rayIntersection(
                    hit - dir1 * dist1, dir1,
                    hit - dir2 * dist2, dir2,
                    result
                )
            )
            assertEquals(Vector2f(dist1, dist2), result, 1e-5)
        }
    }

    @Test
    fun testParallelRaysF() {
        assertFalse(
            rayIntersection(
                Vector3f(-1.0, -1.0, 0.0), Vector3f(1.0, 0.0, 0.0),
                Vector3f(0.0, 1.0, -1.0), Vector3f(1.0, 0.0, 0.0)
            )
        )
    }
}