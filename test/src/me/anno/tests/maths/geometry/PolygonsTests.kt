package me.anno.tests.maths.geometry

import me.anno.maths.Maths.TAU
import me.anno.maths.geometry.Polygons.getPolygonArea2f
import me.anno.maths.geometry.Polygons.getPolygonArea3d
import me.anno.maths.geometry.Polygons.getPolygonAreaVector3d
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.createList
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.sin
import kotlin.random.Random

class PolygonsTests {

    companion object {
        fun createNgon(n: Int): List<Vector2d> {
            return createList(n) {
                Vector2d(1.0, 0.0).rotate(it * TAU / n)
            }
        }

        fun getNgonArea(n: Int): Double {
            return 0.5 * n * sin(TAU / n)
        }
    }

    @Test
    fun testPolygonAreaEmpty2d() {
        assertEquals(0f, getPolygonArea2f(listOf()))
    }

    @Test
    fun testPolygonAreaEmpty3d() {
        assertEquals(Vector3d(), getPolygonAreaVector3d(listOf(), Vector3d()))
        assertEquals(0.0, getPolygonArea3d(listOf()))
    }

    @Test
    fun testPolygonArea2d() {
        for (n in 2 until 10) {
            // try a few polygons with known size
            val points = createNgon(n).map { Vector2f(it) }
            val expectedArea = getNgonArea(n).toFloat()
            assertEquals(+expectedArea, getPolygonArea2f(points), 1e-6f)
            assertEquals(-expectedArea, getPolygonArea2f(points.reversed()), 1e-6f)
        }
    }

    @Test
    fun testPolygonArea3d() {
        val random = Random(1234)
        for (s in 0 until 10) {
            // try a few random transforms
            val transform = Quaterniond().rotateYXZ(
                random.nextDouble() * TAU,
                random.nextDouble() * TAU,
                random.nextDouble() * TAU
            )
            val randomZOffset = random.nextDouble() * 2.0 - 1.0 // shouldn't influence the result
            val zAxis = transform.transform(Vector3d(0.0, 0.0, 1.0))
            for (n in 2 until 10) {
                // try a few polygons with known size
                val points = createNgon(n).map {
                    transform.transform(Vector3d(it.x, it.y, randomZOffset))
                }
                val expectedArea = getNgonArea(n)
                val expectedAreaVec = zAxis.normalize(expectedArea)
                assertEquals(+expectedAreaVec, getPolygonAreaVector3d(points, Vector3d()), 1e-14)
                assertEquals(-expectedAreaVec, getPolygonAreaVector3d(points.reversed(), Vector3d()), 1e-14)
                assertEquals(+expectedArea, getPolygonArea3d(points), 1e-14) // will always be positive
            }
        }
    }
}