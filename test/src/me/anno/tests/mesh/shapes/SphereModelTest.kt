package me.anno.tests.mesh.shapes

import me.anno.ecs.components.mesh.TransformMesh.scale
import me.anno.ecs.components.mesh.TransformMesh.transform
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.sdf.shapes.SDFSphere
import me.anno.tests.mesh.shapes.CapsuleModelTest.Companion.assertMeshEqualsSDF
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class SphereModelTest {
    companion object {
        private const val us = 12
        private const val vs = 12

        private const val radius0 = 0.7f
    }

    @Test
    fun testUVSphere() {
        val mesh = UVSphereModel.createUVSphere(us, vs)
        mesh.scale(Vector3f(radius0))
        val sdf = SDFSphere().apply { scale = radius0 }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -radius0, -radius0, -radius0,
                +radius0, +radius0, +radius0
            ), 1e-3f
        )
    }

    @Test
    fun testIcosahedron() {
        val mesh = IcosahedronModel.createIcosphere(2)
        mesh.scale(Vector3f(radius0))
        val sdf = SDFSphere().apply { scale = radius0 }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -radius0, -radius0, -radius0,
                +radius0, +radius0, +radius0
            ), 1e-3f
        )
    }
}