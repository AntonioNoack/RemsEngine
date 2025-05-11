package me.anno.tests.mesh.shapes

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.CapsuleModel.transformYToAxis
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.sdf.shapes.SDFCylinder
import me.anno.tests.mesh.shapes.CapsuleModelTest.Companion.assertMeshEqualsSDF
import org.joml.AABBf
import org.junit.jupiter.api.Test

class CylinderModelTest {
    companion object {
        private const val us = 12 // must be divisible by four for trivial bounds
        private const val vs = 7

        private const val radius0 = 0.7f
        private const val halfHeight0 = 1.7f
    }

    private fun createCylinder(axis: Axis): Mesh {
        val mesh = CylinderModel.createCylinder(
            us, vs, top = true, bottom = true, null, 1f, Mesh(),
            -halfHeight0, halfHeight0, radius0
        )
        return transformYToAxis(mesh, axis)
    }

    @Test
    fun testCylinderX() {
        val mesh = createCylinder(Axis.X)
        val sdf = SDFCylinder().apply {
            radius = radius0
            halfHeight = halfHeight0
            axis = Axis.X
        }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -halfHeight0, -radius0, -radius0,
                +halfHeight0, +radius0, +radius0
            ), 1f
        )
    }

    @Test
    fun testCylinderY() {
        val mesh = createCylinder(Axis.Y)
        val sdf = SDFCylinder().apply {
            radius = radius0
            halfHeight = halfHeight0
        }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -radius0, -halfHeight0, -radius0,
                +radius0, +halfHeight0, +radius0
            ), 1f
        )
    }

    @Test
    fun testCylinderZ() {
        val mesh = createCylinder(Axis.Z)
        val sdf = SDFCylinder().apply {
            radius = radius0
            halfHeight = halfHeight0
            axis = Axis.Z
        }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -radius0, -radius0, -halfHeight0,
                +radius0, +radius0, +halfHeight0
            ), 1f
        )
    }
}