package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Lines.lineIntersection
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class LineIntersectionTests3D {
    @Test
    fun testSampleD() {
        val result1 = Vector3d()
        val result2 = Vector3d()
        assertTrue(
            lineIntersection(
                Vector3d(-1.0, -1.0, 0.0), Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, -1.0), Vector3d(0.0, 0.0, 1.0),
                result1, result2
            )
        )
        assertEquals(Vector3d(0.0, -1.0, 0.0), result1)
        assertEquals(Vector3d(0.0, 1.0, 0.0), result2)
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

            val result1 = Vector3d()
            val result2 = Vector3d()
            assertTrue(
                lineIntersection(
                    hit + dir1 * dist1, dir1,
                    hit + dir2 * dist2, dir2,
                    result1, result2
                )
            )
            assertEquals(hit, result1, 1e-13)
            assertEquals(hit, result2, 1e-13)
        }
    }

    @Test
    fun testParallelLinesD() {
        assertFalse(
            lineIntersection(
                Vector3d(-1.0, -1.0, 0.0), Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, -1.0), Vector3d(1.0, 0.0, 0.0),
                Vector3d(), Vector3d()
            )
        )
    }

    @Test
    fun testSampleF() {
        val result1 = Vector3f()
        val result2 = Vector3f()
        assertTrue(
            lineIntersection(
                Vector3f(-1.0, -1.0, 0.0), Vector3f(1.0, 0.0, 0.0),
                Vector3f(0.0, 1.0, -1.0), Vector3f(0.0, 0.0, 1.0),
                result1, result2
            )
        )
        assertEquals(Vector3f(0.0, -1.0, 0.0), result1)
        assertEquals(Vector3f(0.0, 1.0, 0.0), result2)
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

            val result1 = Vector3f()
            val result2 = Vector3f()
            assertTrue(
                lineIntersection(
                    hit + dir1 * dist1, dir1,
                    hit + dir2 * dist2, dir2,
                    result1, result2
                )
            )
            assertEquals(hit, result1, 1e-5)
            assertEquals(hit, result2, 1e-5)
        }
    }

    @Test
    fun testParallelLinesF() {
        assertFalse(
            lineIntersection(
                Vector3f(-1.0, -1.0, 0.0), Vector3f(1.0, 0.0, 0.0),
                Vector3f(0.0, 1.0, -1.0), Vector3f(1.0, 0.0, 0.0),
                Vector3f(), Vector3f()
            )
        )
    }
}