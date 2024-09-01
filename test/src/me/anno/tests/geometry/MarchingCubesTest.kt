package me.anno.tests.geometry

import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.MarchingCubes
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.AABBf
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class MarchingCubesTest {

    @Test
    fun testSphere() {
        val w = 10
        val h = 10
        val d = 10
        val r = 5f
        val values = FloatArray(w * h * d)
        val x0 = (w - 1f) * 0.5f
        val y0 = (h - 1) * 0.5f
        val z0 = (d - 1) * 0.5f
        fun sdf(x: Float, y: Float, z: Float): Float {
            return sq(x - x0, y - y0, z - z0) - sq(r)
        }
        for (z in 0 until d) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    values[x + (y + z * h) * w] = sdf(x.toFloat(), y.toFloat(), z.toFloat())
                }
            }
        }
        val triangles0 = MarchingCubes.march(
            w, h, d, values, 0f,
            AABBf(0f, 0f, 0f, w - 1f, h - 1f, d - 1f),
            false
        )
        assertEquals(0, triangles0.size % 3)
        val triangles = (0 until triangles0.size step 3).map {
            Vector3f(triangles0.values, it)
        }
        val surfaceArea = 4f * r * r * PIf
        val guess = (2 * surfaceArea).toInt() // guessing 2 triangles per cube
        val numTriangles = triangles.size / 3
        assertTrue(numTriangles in guess..guess * 5 / 4)
        // check that SDF is zero at points
        for (v in triangles) {
            assertTrue(sdf(v.x, v.y, v.z) < 0.5f)
        }
        // check that side lengths are < sqrt(3)
        for (i in triangles.indices step 3) {
            val a = triangles[i]
            val b = triangles[i + 1]
            val c = triangles[i + 2]
            assertTrue(a.distanceSquared(b) <= 3f)
            assertTrue(b.distanceSquared(c) <= 3f)
            assertTrue(c.distanceSquared(a) <= 3f)
        }
    }
}