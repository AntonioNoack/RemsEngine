package me.anno.tests.gfx

import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Triangles.isInsideTriangle
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2i
import org.junit.jupiter.api.Test
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.assertEquals

class RasterizerTest {

    @Test
    fun testRasterizer() {
        testRasterizer(
            Vector2f(4f, 3f),
            Vector2f(1f, 2f),
            Vector2f(2f, 6f),
            null
        )
        testRasterizer(
            Vector2f(5.5f, 3.1f),
            Vector2f(1.2f, 2.2f),
            Vector2f(2.1f, 6.8f),
            null
        )
        testRasterizer(
            Vector2f(5.5f, 3.1f),
            Vector2f(1.2f, 2.2f),
            Vector2f(2.1f, 6.8f),
            AABBf(3f, 0f, 0f, 5f, 5f, 0f)
        )
    }

    /**
     * test that each pixel is called exactly once
     * */
    fun testRasterizer(a: Vector2f, b: Vector2f, c: Vector2f, testBounds: AABBf?) {
        val bounds = AABBf()
        bounds.union(a).union(b).union(c)
        val pointsInTriangle = HashSet<Vector2i>()
        val minX = floor(bounds.minX).toInt()
        val minY = floor(bounds.minY).toInt()
        val maxX = ceil(bounds.maxX).toInt()
        val maxY = ceil(bounds.maxY).toInt()
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val pt = Vector2f(x, y)
                if (testBounds == null || testBounds.testPoint(pt.x, pt.y, 0f)) {
                    if (pt.isInsideTriangle(a, b, c)) {
                        pointsInTriangle.add(Vector2i(x, y))
                    }
                }
            }
        }
        // println("Points: $pointsInTriangle")
        assertTrue(pointsInTriangle.size > 0)
        Rasterizer.rasterize(a, b, c, testBounds) { minXi, maxXi, y ->
            for (x in minXi..maxXi) {
                assertTrue(pointsInTriangle.remove(Vector2i(x, y)), "Missing ($x,$y)")
            }
        }
        assertEquals(emptySet(), pointsInTriangle)
    }
}