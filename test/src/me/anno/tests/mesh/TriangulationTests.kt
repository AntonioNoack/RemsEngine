package me.anno.tests.mesh

import me.anno.image.ImageWriter
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.Polygons.getPolygonArea2d
import me.anno.maths.geometry.Polygons.getPolygonArea2f
import me.anno.maths.geometry.Polygons.getPolygonArea3d
import me.anno.mesh.Triangulation.ringToTrianglesVec2f
import me.anno.mesh.Triangulation.ringToTrianglesVec3d
import me.anno.tests.maths.geometry.PolygonsTests.Companion.createNgon
import me.anno.tests.maths.geometry.PolygonsTests.Companion.getNgonArea
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotContains
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Triangles
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector2d.Companion.lengthSquared
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import org.the3deers.util.EarCut
import kotlin.random.Random

class TriangulationTests {

    @Test
    fun testEmpty2f() {
        assertEquals(emptyList<Vector2f>(), ringToTrianglesVec2f(emptyList()))
    }

    @Test
    fun testSimple2f() {
        for (n in 2 until 10) {
            val points = createNgon(n).map { Vector2f(it) }
            val triangulation = ringToTrianglesVec2f(points)
            assertEquals((n - 2) * 3, triangulation.size)
            val expectedArea = getPolygonArea2f(points)
            val totalArea = triangulation.indices.step(3).sumOf {
                getPolygonArea2f(triangulation.subList(it, it + 3)).toDouble()
            }.toFloat()
            assertEquals(expectedArea, totalArea, 1e-6f)
        }
    }

    @Test
    fun testEmpty3d() {
        assertEquals(emptyList<Vector3d>(), ringToTrianglesVec3d(emptyList()))
    }

    @Test
    fun testSimple3d() {
        val random = Random(1234L)
        for (s in 0 until 10) {
            // try a few random transforms
            val transform = Quaterniond().rotateYXZ(
                random.nextDouble() * TAU,
                random.nextDouble() * TAU,
                random.nextDouble() * TAU
            )
            val randomZOffset = random.nextDouble() * 2.0 - 1.0 // shouldn't influence the result
            for (n in 2 until 10) {
                val points = createNgon(n).map {
                    transform.transform(Vector3d(it.x, it.y, randomZOffset))
                }
                val triangulation = ringToTrianglesVec3d(points)
                assertEquals((n - 2) * 3, triangulation.size)
                val expectedArea = getPolygonArea3d(points)
                val totalArea = triangulation.indices.step(3).sumOf {
                    getPolygonArea3d(triangulation.subList(it, it + 3))
                }
                assertEquals(expectedArea, totalArea, 1e-14)
            }
        }
    }

    @Test
    fun testConcaveShape() {

        val rnd = Random(123)
        val points = ArrayList<Vector2d>()

        val n = 35
        for (i in 0 until n) {
            val angle = i * TAU / n
            val radius = rnd.nextDouble()
            points.add(Vector2d(radius, 0.0).rotate(angle))
        }

        val data = DoubleArray(points.size * 2)
        for (i in points.indices) {
            val point = points[i]
            data[i * 2] = point.x
            data[i * 2 + 1] = point.y
        }

        val triangles = EarCut.earcut(data, 2)!!
        if (false) ImageWriter.writeTriangles(512, "concave.png", points.map { Vector2f(it) }, triangles.toIntArray())

        // validate shape
        // each signed edge must appear correctly once
        val actualSignedEdges = HashSet<Pair<Vector2d, Vector2d>>()
        fun put(a: Int, b: Int, dst: HashSet<Pair<Vector2d, Vector2d>>) {
            assertTrue(dst.add(points[a] to points[b]))
        }

        fun put(a: Int, b: Int, c: Int) {
            put(a, b, actualSignedEdges)
            put(b, c, actualSignedEdges)
            put(c, a, actualSignedEdges)
        }
        for (i in triangles.indices step 3) {
            put(triangles[i], triangles[i + 1], triangles[i + 2])
        }

        // check outer circle
        for (i in 0 until n) {
            val a = points[i]
            val b = points[(i + 1) % n]
            assertContains(a to b, actualSignedEdges)
            assertNotContains(b to a, actualSignedEdges)
        }

        val expectedArea = getPolygonArea2d(points)
        val actualArea = (triangles.indices step 3).sumOf { i ->
            val a = points[triangles[i]]
            val b = points[triangles[i + 1]]
            val c = points[triangles[i + 2]]
            Triangles.getTriangleArea(a, b, c)
        }
        assertEquals(expectedArea, actualArea, 1e-15 * expectedArea)
    }

    @Test
    fun testShapeWithHoles() {

        val rnd = Random(123)
        val points = ArrayList<Vector2d>()
        val holeIndices = IntArrayList(16)

        val n = 7
        fun addCircle(pos: Vector2d, radius: Double, inside: Boolean) {
            if (inside) holeIndices.add(points.size) // start indices for holes need to be given
            val da = rnd.nextDouble() * TAU
            for (i in 0 until n) {
                val angle = da + i * TAU / n
                points.add(Vector2d(radius, 0.0).rotate(angle).add(pos))
            }
        }

        fun addCircle(pos: Vector3d, inside: Boolean) {
            addCircle(Vector2d(pos.x, pos.y), pos.z, inside)
        }

        fun collides(c1: Vector3d, c2: Vector3d): Boolean {
            return lengthSquared(c1.x - c2.x, c1.y - c2.y) < sq(c1.z + c2.z)
        }

        val outerCircle = Vector3d(0.0, 0.0, 10.0)
        addCircle(outerCircle, false)
        val innerCircles = ArrayList<Vector3d>()
        for (i in 0 until 35) {
            val circle = Vector3d()
            do {
                circle.set(
                    rnd.nextDouble() * 8.0, 0.0,
                    mix(0.5, 1.0, rnd.nextDouble()) * 1.5
                ).rotateZ(rnd.nextDouble() * TAU)
            } while (innerCircles.any { collides(circle, it) })
            addCircle(circle, true)
            innerCircles.add(circle)
        }

        val data = DoubleArray(points.size * 2)
        for (i in points.indices) {
            val point = points[i]
            data[i * 2] = point.x
            data[i * 2 + 1] = point.y
        }

        val triangles = EarCut.earcut(data, holeIndices.toIntArray(), 2)!!
        if (false) ImageWriter.writeTriangles(512, "withHoles.png", points.map { Vector2f(it) }, triangles.toIntArray())

        // validate shape
        // each signed edge must appear correctly once
        val actualSignedEdges = HashSet<Pair<Vector2d, Vector2d>>()
        fun put(a: Int, b: Int, dst: HashSet<Pair<Vector2d, Vector2d>>) {
            assertTrue(dst.add(points[a] to points[b]))
        }

        fun put(a: Int, b: Int, c: Int) {
            put(a, b, actualSignedEdges)
            put(b, c, actualSignedEdges)
            put(c, a, actualSignedEdges)
        }
        for (i in triangles.indices step 3) {
            put(triangles[i], triangles[i + 1], triangles[i + 2])
        }

        // check outer circle
        for (i in 0 until n) {
            val a = points[i]
            val b = points[(i + 1) % n]
            assertContains(a to b, actualSignedEdges)
            assertNotContains(b to a, actualSignedEdges)
        }

        // check inner circles are properly connected
        for (j in holeIndices.indices) {
            val j0 = holeIndices[j]
            for (di in 0 until n) {
                val a = points[j0 + di]
                val b = points[j0 + (di + 1) % n]
                assertContains(b to a, actualSignedEdges)
                assertNotContains(a to b, actualSignedEdges)
            }
        }

        // check area
        fun getArea(circle: Vector3d): Double {
            return getNgonArea(n) * sq(circle.z)
        }

        val expectedArea = getArea(outerCircle) - innerCircles.sumOf { getArea(it) }
        val actualArea = (triangles.indices step 3).sumOf { i ->
            val a = points[triangles[i]]
            val b = points[triangles[i + 1]]
            val c = points[triangles[i + 2]]
            Triangles.getTriangleArea(a, b, c)
        }
        assertEquals(expectedArea, actualArea, 1e-15 * expectedArea)
    }
}