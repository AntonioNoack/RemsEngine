package me.anno.tests.maths.geometry

import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.DualContouring
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2f
import org.junit.jupiter.api.Test

class DualContouringTests {
    companion object {
        // surely, we have this implemented already somewhere, find it
        // -> only an ugly version in EarCut operating on FloatArray
        // todo this should be put somewhere into the main module...
        fun getPolygonArea(points: List<Vector2f>): Float {
            var sum = 0f
            for (j in points.indices) {
                val i = if (j == 0) points.lastIndex else j - 1
                val ni = points[i]
                val nj = points[j]
                sum += ni.cross(nj)
            }
            return 0.5f * sum
        }
    }

    @Test
    fun testPointsOnZero() {
        val dx = 8
        val r = 6f
        val ptsList = DualContouring.contour2d(dx * 2 + 1, dx * 2 + 1, { x, y ->
            val xi = x - dx
            val yi = y - dx
            xi * xi + yi * yi - r * r
        })

        assertEquals(1, ptsList.size)
        val pts = ptsList.first()

        val expectedSize = r * TAUf
        assertTrue(pts.size.toFloat() in expectedSize..expectedSize * 1.5f)

        val expectedArea = sq(r) * PIf // 113.1
        val actualArea = getPolygonArea(pts) // -112.3
        assertTrue(-actualArea in expectedArea * 0.98f..expectedArea * 1.02f)
    }
}