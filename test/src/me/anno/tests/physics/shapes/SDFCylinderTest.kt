package me.anno.tests.physics.shapes

import me.anno.bullet.createBulletCylinderShape
import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.maths.Maths.TAU
import me.anno.sdf.SDFCollider
import me.anno.sdf.physics.ConvexSDFShape
import me.anno.sdf.shapes.SDFCylinder
import me.anno.tests.physics.shapes.SDFSphereTest.Companion.nextPos
import me.anno.tests.physics.shapes.SDFSphereTest.Companion.toKOML
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SDFCylinderTest {

    @Test
    fun testSDFSupportVectorWithoutMargin() {
        testSDFSupportVector(Axis.X, 0f)
        testSDFSupportVector(Axis.Y, 0f)
        testSDFSupportVector(Axis.Z, 0f)
    }

    @Test
    fun testSDFSupportVectorWithMargin() {
        testSDFSupportVector(Axis.X, 1f)
        testSDFSupportVector(Axis.Y, 1f)
        testSDFSupportVector(Axis.Z, 1f)
    }

    fun testSDFSupportVector(axisI: Axis, marginI: Float) {
        val baseline = CylinderCollider().apply {
            axis = axisI
            roundness = marginI
        }.createBulletCylinderShape(Vector3d(1.0))
        assertEquals(marginI, baseline.margin.toFloat())
        val tested = ConvexSDFShape(
            SDFCylinder().apply {
                axis = axisI
                halfHeight = 1f + marginI
                radius = 1f + marginI
                smoothness = marginI
                localAABB
                    .setMin(-1.0, -1.0, -1.0)
                    .setMax(1.0, 1.0, 1.0)
            }, SDFCollider()
        )
        tested.margin = 0.0
        val random = Random(1234)
        val accurate = marginI == 0f
        // it's a shame that we have to use soo big margins :/
        val threshold = if (accurate) 1e-5 else 1.0 + marginI
        for (i in 0 until 100) {
            val pos = random.nextPos()

            // if possible, use smaller margins by spawning a point on the surface
            if (accurate) {
                val angle = random.nextDouble() * TAU
                val y = if (random.nextBoolean()) 1.0 else -1.0
                when (axisI) {
                    Axis.X -> {
                        pos.x = y
                        pos.y = cos(angle)
                        pos.z = sin(angle)
                    }
                    Axis.Y -> {
                        pos.y = y
                        pos.x = cos(angle)
                        pos.z = sin(angle)
                    }
                    Axis.Z -> {
                        pos.z = y
                        pos.x = cos(angle)
                        pos.y = sin(angle)
                    }
                }
            }

            val expected = baseline.localGetSupportingVertex(pos, Vector3d()).toKOML()
            val actual = tested.localGetSupportingVertex(pos, Vector3d()).toKOML()
            assertEquals(expected.x, actual.x, threshold)
            assertEquals(expected.y, actual.y, threshold)
            assertEquals(expected.z, actual.z, threshold)
        }
    }
}