package com.bulletphysics.linearmath.convexhull

import me.anno.maths.geometry.convexhull.ConvexHull
import me.anno.maths.geometry.convexhull.ConvexHulls.Companion.calculateConvexHullNaive
import me.anno.maths.geometry.convexhull.HullDesc
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3i
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.abs

class ConvexHull3DTest {

    /**
     * Helper to create Vector3d from floats
     * */
    private fun v(x: Float, y: Float, z: Float): Vector3d {
        return Vector3d(x.toDouble(), y.toDouble(), z.toDouble())
    }

    @Test
    fun testTetrahedron() {
        val pts = listOf(
            v(0f, 0f, 0f), v(1f, 0f, 0f), v(0f, 1f, 0f), v(0f, 0f, 1f)
        )

        val hull = calculateConvexHullNaive(HullDesc(pts))!!
        val vertices = hull.vertices
        val triangles = hull.triangles

        // The hull of tetrahedron input should have exactly these vertices
        assertEquals(4, vertices.size)
        assertEquals(12, triangles.size)

        for (p in pts) {
            val found = vertices.any { v -> v == p }
            assertTrue(found, "Hull should contain original point $p")
        }

        // check the faces to be what we expect
        val expectedFaces = listOf(
            pts[0], pts[2], pts[1],
            pts[0], pts[3], pts[2],
            pts[0], pts[1], pts[3]
        )

        val actualFaces = (triangles.indices step 3).map { i ->
            Triple(
                vertices[triangles[i]],
                vertices[triangles[i + 1]],
                vertices[triangles[i + 2]]
            )
        }

        forLoopSafely(expectedFaces.size, 3) { i ->
            val a = expectedFaces[i]
            val b = expectedFaces[i + 1]
            val c = expectedFaces[i + 2]
            assertTrue(
                Triple(a, b, c) in actualFaces ||
                        Triple(b, c, a) in actualFaces ||
                        Triple(c, a, b) in actualFaces
            )
        }
    }

    @Test
    fun testCubeCorners() {
        val pts = listOf(
            v(0f, 0f, 0f), v(1f, 0f, 0f), v(1f, 1f, 0f), v(0f, 1f, 0f),
            v(0f, 0f, 1f), v(1f, 0f, 1f), v(1f, 1f, 1f), v(0f, 1f, 1f)
        )

        val hull = calculateConvexHullNaive(HullDesc(pts))!!
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

        val hull = calculateConvexHullNaive(HullDesc(pts))!!
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
        repeat(100) {
            pts.add(Vector3d(rnd.nextFloat().toDouble(), rnd.nextFloat().toDouble(), rnd.nextFloat().toDouble()))
        }

        val hull1 = calculateConvexHullNaive(HullDesc(pts))!!
        val hull2 = calculateConvexHullNaive(HullDesc(pts))!!

        val verts1 = ArrayList(hull1.vertices)
        val verts2 = ArrayList(hull2.vertices)

        // Sort and compare vertices for equality
        val cmp = Comparator
            .comparingDouble { v: Vector3d -> v.x }
            .thenComparingDouble { v: Vector3d -> v.y }
            .thenComparingDouble { v: Vector3d -> v.z }

        verts1.sortWith(cmp)
        verts2.sortWith(cmp)

        Assertions.assertEquals(verts1.size, verts2.size)

        for (i in verts1.indices) {
            val a = verts1[i]
            val b = verts2[i]
            Assertions.assertTrue(a.epsilonEquals(b, 1e-6))
        }
    }

    private fun generateSphereHull(n: Int): ConvexHull {
        val rnd = Random(123)
        val pts = ArrayList<Vector3d>()
        repeat(100) {
            pts.add(Vector3d(rnd.nextFloat().toDouble(), rnd.nextFloat().toDouble(), rnd.nextFloat().toDouble()))
        }

        return calculateConvexHullNaive(HullDesc(pts))!!
    }

    @Test
    fun testTriangleCountIsApproximatelyCorrect() {
        val hull = generateSphereHull(100)
        val triangles = hull.triangles
        // check number of triangles is approximately the same as vertices * 5/3
        assertTrue(abs(triangles.size - hull.vertices.size * 5) < triangles.size / 5)
        // check number of triangle indices is divisible by 3
        assertEquals(triangles.size % 3, 0)
    }

    @Test
    fun testTrianglesAreNotDegenerate() {
        val triangles = generateSphereHull(100).triangles
        forLoopSafely(triangles.size, 3) { i ->
            val ai = triangles[i]
            val bi = triangles[i + 1]
            val ci = triangles[i + 2]
            assertNotEquals(ai, bi)
            assertNotEquals(bi, ci)
            assertNotEquals(ci, ai)
        }
    }

    @Test
    fun testTriangleHullIsProbablyClosed() {
        val hull = generateSphereHull(100)
        val triangles = hull.triangles
        val coveredVertices = IntArray(hull.vertices.size)
        // all vertices must appear at least three times for a closed shape
        forLoopSafely(triangles.size, 3) { i ->
            coveredVertices[triangles[i]]++
            coveredVertices[triangles[i + 1]]++
            coveredVertices[triangles[i + 2]]++
        }
        assertTrue(coveredVertices.all { it >= 3 })
    }

    @Test
    fun testTrianglesAreUnique() {
        val triangles = generateSphereHull(100).triangles
        // all triangles must be unique
        val uniqueTriangles = HashSet<Vector3i>(triangles.size / 3)
        forLoopSafely(triangles.size, 3) { i ->
            val v = Vector3i(triangles, i)
            assertTrue(uniqueTriangles.add(v))
        }
        assertEquals(uniqueTriangles.size * 3, triangles.size)
    }

    private fun Vector3d.epsilonEquals(v: Vector3d, delta: Double): Boolean {
        val dx = x - v.x
        val dy = y - v.y
        val dz = z - v.z
        return abs(dx) <= delta && abs(dy) <= delta && abs(dz) <= delta
    }
}