package me.anno.tests.mesh

import me.anno.maths.Maths.TAU
import me.anno.maths.geometry.Polygons.getPolygonArea2f
import me.anno.maths.geometry.Polygons.getPolygonArea3d
import me.anno.mesh.Triangulation.ringToTrianglesVec2f
import me.anno.mesh.Triangulation.ringToTrianglesVec3d
import me.anno.tests.maths.geometry.PolygonsTests.Companion.createNgon
import me.anno.utils.assertions.assertEquals
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Test
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

    // todo test with holes?
    // todo test with concave polygon
}