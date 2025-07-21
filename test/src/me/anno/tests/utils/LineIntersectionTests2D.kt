package me.anno.tests.utils

import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.TAUf
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.LineIntersection.lineIntersection
import org.joml.Vector2d
import org.joml.Vector2f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class LineIntersectionTests2D {
    @Test
    fun testSampleD() {
        val result1 = Vector2d()
        val result2 = Vector2d()
        assertTrue(
            lineIntersection(
                Vector2d(-1.0, 0.0), Vector2d(1.0, 0.0),
                Vector2d(0.0, -1.0), Vector2d(0.0, 1.0),
                result1, result2
            )
        )
        assertEquals(Vector2d(0.0, 0.0), result1)
        assertEquals(Vector2d(0.0, 0.0), result2)
    }

    @Test
    fun testRandomSamplesD() {
        val rnd = Random(1634)
        repeat(20) {
            val hit = Vector2d()
            val dist1 = (rnd.nextDouble() - 0.5) * 10.0
            val dist2 = (rnd.nextDouble() - 0.5) * 10.0
            val dir1 = Vector2d(0.0, 1.0).rotate(rnd.nextDouble() * TAU)
            val dir2 = Vector2d(0.0, 1.0).rotate(rnd.nextDouble() * TAU)

            val result1 = Vector2d()
            val result2 = Vector2d()
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
                Vector2d(-1.0, 0.0), Vector2d(1.0, 0.0),
                Vector2d(0.0, -1.0), Vector2d(1.0, 0.0),
                Vector2d(), Vector2d()
            )
        )
    }

    @Test
    fun testSampleF() {
        val result1 = Vector2f()
        val result2 = Vector2f()
        assertTrue(
            lineIntersection(
                Vector2f(-1.0, 0.0), Vector2f(1.0, 0.0),
                Vector2f(0.0, -1.0), Vector2f(0.0, 1.0),
                result1, result2
            )
        )
        assertEquals(Vector2f(0.0, 0.0), result1)
        assertEquals(Vector2f(0.0, 0.0), result2)
    }

    @Test
    fun testRandomSamplesF() {
        val rnd = Random(1634)
        repeat(20) {
            val hit = Vector2f()
            val dist1 = (rnd.nextFloat() - 0.5f) * 10f
            val dist2 = (rnd.nextFloat() - 0.5f) * 10f
            val dir1 = Vector2f(0f, 1f).rotate(rnd.nextFloat() * TAUf)
            val dir2 = Vector2f(0f, 1f).rotate(rnd.nextFloat() * TAUf)

            val result1 = Vector2f()
            val result2 = Vector2f()
            assertTrue(
                lineIntersection(
                    hit + dir1 * dist1, dir1,
                    hit + dir2 * dist2, dir2,
                    result1, result2
                )
            )
            assertEquals(hit, result1, 1e-4)
            assertEquals(hit, result2, 1e-4)
        }
    }

    @Test
    fun testParallelLinesF() {
        assertFalse(
            lineIntersection(
                Vector2f(-1.0, 0.0), Vector2f(1.0, 0.0),
                Vector2f(0.0, -1.0), Vector2f(1.0, 0.0),
                Vector2f(), Vector2f()
            )
        )
    }
}