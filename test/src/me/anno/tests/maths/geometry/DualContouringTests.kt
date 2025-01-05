package me.anno.tests.maths.geometry

import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.DualContouring
import me.anno.maths.geometry.Polygons.getPolygonArea2f
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class DualContouringTests {
    @Test
    fun testPointsOnZero() {
        val dx = 8
        val r = 6f
        val ptsList = DualContouring
            .contour2d(dx * 2 + 1, dx * 2 + 1, { x, y ->
                val xi = x - dx
                val yi = y - dx
                xi * xi + yi * yi - r * r
            })

        assertEquals(1, ptsList.size)
        val pts = ptsList.first()

        val expectedSize = r * TAUf
        assertTrue(pts.size.toFloat() in expectedSize..expectedSize * 1.5f)

        val expectedArea = sq(r) * PIf // 113.1
        val actualArea = getPolygonArea2f(pts) // 112.3
        assertTrue(actualArea in expectedArea * 0.98f..expectedArea * 1.02f)
    }
}