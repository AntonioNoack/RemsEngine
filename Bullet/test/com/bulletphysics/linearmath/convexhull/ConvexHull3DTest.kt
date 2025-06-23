package com.bulletphysics.linearmath.convexhull

import me.anno.utils.assertions.assertNotNull
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Random
import java.util.function.ToDoubleFunction
import kotlin.math.abs

class ConvexHull3DTest {
    // Helper to create Vector3d from floats
    private fun v(x: Float, y: Float, z: Float): Vector3d {
        return Vector3d(x.toDouble(), y.toDouble(), z.toDouble())
    }

    private class ConvexHull3D(pts: List<Vector3d>) {
        val vertices: List<Vector3d>

        init {
            val desc = HullDesc(pts, pts.size)
            val result = assertNotNull(ConvexHulls.calculateConvexHull(desc))
            this.vertices = result.vertices
        }
    }

    @Test
    fun testTetrahedron() {
        val pts = listOf(
            v(0f, 0f, 0f), v(1f, 0f, 0f), v(0f, 1f, 0f), v(0f, 0f, 1f)
        )
        val hull = ConvexHull3D(pts)

        val vertices = hull.vertices

        // The hull of tetrahedron input should have exactly these vertices
        Assertions.assertEquals(4, vertices.size)

        for (p in pts) {
            val found = vertices.any { v -> v == p }
            Assertions.assertTrue(found, "Hull should contain original point $p")
        }
    }

    @Test
    fun testCubeCorners() {
        val pts = listOf(
            v(0f, 0f, 0f), v(1f, 0f, 0f), v(1f, 1f, 0f), v(0f, 1f, 0f),
            v(0f, 0f, 1f), v(1f, 0f, 1f), v(1f, 1f, 1f), v(0f, 1f, 1f)
        )
        val hull = ConvexHull3D(pts)

        val vertices = hull.vertices
        Assertions.assertEquals(8, vertices.size)

        // All input points should be on hull
        for (p in pts) {
            val found = vertices.any { v -> v == p }
            Assertions.assertTrue(found)
        }
    }

    @Test
    fun testDuplicatePoints() {
        val pts = listOf(
            v(0f, 0f, 0f), v(1f, 0f, 0f), v(0f, 1f, 0f), v(0f, 0f, 1f),
            v(0f, 0f, 0f), v(1f, 0f, 0f) // duplicates
        )
        val hull = ConvexHull3D(pts)

        val vertices = hull.vertices

        // Should be 4 unique vertices
        Assertions.assertEquals(4, vertices.size)

        // All unique points appear
        val uniqueInput = listOf(
            v(0f, 0f, 0f), v(1f, 0f, 0f),
            v(0f, 1f, 0f), v(0f, 0f, 1f)
        )
        for (p in uniqueInput) {
            val found = vertices.any { v -> v == p }
            Assertions.assertTrue(found)
        }
    }

    @Test
    fun testRandomPointsConsistency() {
        val rnd = Random(123)
        val pts = ArrayList<Vector3d>()
        for (i in 0..99) {
            pts.add(Vector3d(rnd.nextFloat().toDouble(), rnd.nextFloat().toDouble(), rnd.nextFloat().toDouble()))
        }
        val hull1 = ConvexHull3D(pts)
        val hull2 = ConvexHull3D(pts)

        val verts1 = ArrayList(hull1.vertices)
        val verts2 = ArrayList(hull2.vertices)

        // Sort and compare vertices for equality
        val cmp = Comparator
            .comparingDouble(ToDoubleFunction { v: Vector3d -> v.x })
            .thenComparingDouble(ToDoubleFunction { v: Vector3d -> v.y })
            .thenComparingDouble(ToDoubleFunction { v: Vector3d -> v.z })

        verts1.sortWith(cmp)
        verts2.sortWith(cmp)

        Assertions.assertEquals(verts1.size, verts2.size)

        for (i in verts1.indices) {
            val a = verts1[i]
            val b = verts2[i]
            Assertions.assertTrue(a.epsilonEquals(b, 1e-6))
        }
    }

    private fun Vector3d.epsilonEquals(v: Vector3d, delta: Double): Boolean {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return abs(dx) <= delta && abs(dy) <= delta && abs(dz) <= delta
    }
}