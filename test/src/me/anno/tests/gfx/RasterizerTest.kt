package me.anno.tests.gfx

import me.anno.maths.geometry.Rasterizer
import me.anno.tests.maths.DistancesTest.Companion.testAllAxes3f
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Triangles.isInsideTriangle
import me.anno.utils.types.Triangles.subCross
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
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
    }

    @Test
    fun testRasterizerBounded() {
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
        // corners aren't really inside the triangle, but they should still be part of the rasterized result
        if (isIntCorner(a)) pointsInTriangle.add(Vector2i(a.x.toInt(), a.y.toInt()))
        if (isIntCorner(b)) pointsInTriangle.add(Vector2i(b.x.toInt(), b.y.toInt()))
        if (isIntCorner(c)) pointsInTriangle.add(Vector2i(c.x.toInt(), c.y.toInt()))
        assertTrue(pointsInTriangle.size > 0)
        Rasterizer.rasterize(a, b, c, testBounds) { minXi, maxXi, y ->
            for (x in minXi..maxXi) {
                assertTrue(pointsInTriangle.remove(Vector2i(x, y)), "Missing ($x,$y)")
            }
        }
        assertEquals(emptySet(), pointsInTriangle)
    }

    private fun isIntCorner(a: Vector2f): Boolean {
        return isIntCorner(a.x) && isIntCorner(a.y)
    }

    private fun isIntCorner(a: Float): Boolean {
        return a.toInt().toFloat() == a
    }

    @Test
    fun testRasterizer3dCountVoxels() {
        // check that the number of voxels is approximately as expected
        val sizesCount = HashSet<Int>()
        testAllAxes3f(
            Vector3f(0f, 0f, 7f),
            Vector3f(20f, 0f, 0f),
            Vector3f(0f, 20f, 0f)
        ) { (a, b, c) ->
            val area = subCross(a, b, c, Vector3f()).length() * 0.5f
            sizesCount += testRasterizer3d(
                a, b, c, null,
                (area * 0.95f).toInt()..(area * 1.05f).toInt()
            )
        }
        assertEquals(1, sizesCount.size)
    }

    @Test
    fun testRasterizer3dInBetweenValues() {
        // test floating between two values
        val sizesCount = HashSet<Int>()
        testAllAxes3f(
            Vector3f(0f, 0f, 0.5f),
            Vector3f(20f, 0f, 0.5f),
            Vector3f(0f, 20f, 0.5f)
        ) { (a, b, c) ->
            val area = subCross(a, b, c, Vector3f()).length() * 0.5f
            sizesCount += testRasterizer3d(
                a, b, c, null,
                (area * 0.8f).toInt()..(area * 1.2f).toInt()
            )
        }
        assertEquals(1, sizesCount.size)
    }

    @Test
    fun testRasterizer3dSimplePlanes() {
        // check simple edge cases
        testAllAxes3f(
            Vector3f(),
            Vector3f(20f, 0f, 0f),
            Vector3f(0f, 20f, 0f)
        ) { (a, b, c) ->
            testRasterizer3d(
                a, b, c,
                AABBf().union(a).union(b).union(c).scale(0.5f),
                11 * 11
            )
        }
    }

    fun testRasterizer3d(a: Vector3f, b: Vector3f, c: Vector3f, bounds: AABBf?, allowedRange: IntRange): Int {
        val found = HashSet<Vector3i>()
        Rasterizer.rasterize(a, b, c, bounds) { x, y, z ->
            assertTrue(found.add(Vector3i(x, y, z)))
        }
        assertContains(found.size, allowedRange)
        return found.size
    }

    fun testRasterizer3d(a: Vector3f, b: Vector3f, c: Vector3f, bounds: AABBf?, expectedCount: Int): Int {
        val found = HashSet<Vector3i>()
        Rasterizer.rasterize(a, b, c, bounds) { x, y, z ->
            assertTrue(found.add(Vector3i(x, y, z)))
        }
        assertEquals(found.size, expectedCount)
        return found.size
    }
}