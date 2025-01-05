package me.anno.tests.maths.geometry

import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.MarchingSquares
import me.anno.maths.geometry.Polygons.getPolygonArea2f
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class MarchingSquaresTests {
    @Test
    fun testPointsOnZero() {

        val dx = 8
        val r = 6f

        val ptsList = MarchingSquares.march(
            dx * 2 + 1, dx * 2 + 1,
            FloatArray(sq(dx * 2 + 1)) {
                val x = it % (dx * 2 + 1)
                val y = it / (dx * 2 + 1)
                val xi = x - dx
                val yi = y - dx
                xi * xi + yi * yi - r * r
            }, 0f
        )

        assertEquals(1, ptsList.size)
        val pts = ptsList.first()

        val expectedSize = r * TAUf
        assertTrue(pts.size.toFloat() in expectedSize..expectedSize * 1.5f)

        val expectedArea = sq(r) * PIf // 113.1
        val actualArea = getPolygonArea2f(pts) // 112.1
        assertTrue(actualArea in expectedArea * 0.97f..expectedArea * 1.03f)
    }
}