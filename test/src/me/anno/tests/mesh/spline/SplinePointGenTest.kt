package me.anno.tests.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.Splines
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertLessThan
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.abs

class SplinePointGenTest {

    @Test
    fun testPointGeneration() {

        val root = Entity()
        fun addPoint(v: Vector3d, a: Double) {
            Entity(root)
                .setPosition(v)
                .setRotation(0.0, a.toRadians(), 0.0)
                .add(SplineControlPoint())
        }
        addPoint(Vector3d(-30.0, 0.0, -20.0), 180.0)
        addPoint(Vector3d(0.0, 0.0, 10.0), -50.0)
        addPoint(Vector3d(40.0, 0.0, 0.0), -90.0)
        root.validateTransform()

        val controlPoints = root.children.mapNotNull { it.getComponent(SplineControlPoint::class) }
        assertEquals(3, controlPoints.size)
        val ppr = 10.0
        val points = Splines.generateSplinePoints(controlPoints, ppr, false)
        val maxAngle = 2.0 / ppr

        // test general flow
        for (i in 4 until points.size) {
            val p0 = points[i - 4]
            val p1 = points[i - 2]
            val p2 = points[i]
            assertLessThan(
                abs((p1 - p0).angle(p2 - p1)), maxAngle,
                "$p0 - $p1 - $p2"
            )
        }
    }
}