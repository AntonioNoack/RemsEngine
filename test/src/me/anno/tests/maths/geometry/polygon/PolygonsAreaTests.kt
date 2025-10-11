package me.anno.tests.maths.geometry.polygon

import me.anno.maths.Maths
import me.anno.maths.geometry.polygon.PolygonArea
import me.anno.maths.geometry.polygon.PolygonArea.getPolygonArea2f
import me.anno.maths.geometry.polygon.PolygonArea.getPolygonAreaVector3d
import me.anno.utils.assertions.assertEquals
import org.joml.Quaterniond
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.sin
import kotlin.random.Random

class PolygonsAreaTests {

    companion object {
        fun createNgon(n: Int): Array<Vector2d> {
            return Array(n) {
                Vector2d(1.0, 0.0).rotate(it * Maths.TAU / n)
            }
        }

        fun getNgonArea(n: Int): Double {
            return 0.5 * n * sin(Maths.TAU / n)
        }
    }

    @Test
    fun testPolygonAreaEmpty2d() {
        assertEquals(0f, emptyList<Vector2f>().getPolygonArea2f())
    }

    @Test
    fun testPolygonAreaEmpty3d() {
        assertEquals(Vector3d(), listOf<Vector3d>().getPolygonAreaVector3d(Vector3d()))
        assertEquals(0.0, PolygonArea.getPolygonArea3d(listOf()))
    }

    @Test
    fun testPolygonArea2d() {
        for (n in 2 until 10) {
            // try a few polygons with known size
            val points = createNgon(n).map { Vector2f(it) }
            val expectedArea = getNgonArea(n).toFloat()
            assertEquals(+expectedArea, points.getPolygonArea2f(), 1e-6f)
            assertEquals(-expectedArea, points.reversed().getPolygonArea2f(), 1e-6f)
        }
    }

    @Test
    fun testPolygonArea3d() {
        val random = Random(1234)
        repeat(10) {
            // try a few random transforms
            val transform = Quaterniond().rotateYXZ(
                random.nextDouble() * Maths.TAU,
                random.nextDouble() * Maths.TAU,
                random.nextDouble() * Maths.TAU
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
                assertEquals(+expectedAreaVec, points.getPolygonAreaVector3d(Vector3d()), 1e-14)
                assertEquals(-expectedAreaVec, points.reversed().getPolygonAreaVector3d(Vector3d()), 1e-14)
                assertEquals(+expectedArea, PolygonArea.getPolygonArea3d(points), 1e-14) // will always be positive
            }
        }
    }
}