package me.anno.tests.mesh.shapes

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachPointIndex
import me.anno.ecs.components.mesh.shapes.CapsuleModel
import me.anno.ecs.components.mesh.shapes.CapsuleModel.transformYToAxis
import me.anno.sdf.SDFComponent
import me.anno.sdf.shapes.SDFCapsule
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import org.junit.jupiter.api.Test

class CapsuleModelTest {
    companion object {
        fun assertMeshEqualsSDF(
            mesh: Mesh, sdf: SDFComponent, epsilon: Float, expectedBounds: AABBf,
            normalEpsilon: Float
        ) {
            val pos = Vector3f()
            val nor = Vector3f()
            val normal = Vector3f()
            val posAndOffset = Vector4f()

            val seeds = IntArrayList(0)
            val bounds = AABBf()

            val positions = mesh.positions!!
            val normals = mesh.normals!!
            mesh.forEachPointIndex(false) { i ->

                // prepare
                pos.set(positions, i * 3)
                nor.set(normals, i * 3)
                posAndOffset.set(pos, 0f)

                // validate signed distance
                val distance = sdf.computeSDF(posAndOffset, seeds)
                assertEquals(0f, distance, epsilon)

                // validate normal
                sdf.computeNormal(pos, normal, seeds)
                assertEquals(normal, nor, normalEpsilon.toDouble())

                // add point to bounds
                bounds.union(pos)
                false
            }

            // validate bounds
            assertEquals(expectedBounds, bounds, epsilon.toDouble())
        }

        private const val us = 12 // must be divisible by four for trivial bounds
        private const val vs = 7

        private const val radius0 = 0.7f
        private const val halfHeight = 1.7f
    }

    @Test
    fun testCapsuleX() {
        val mesh = CapsuleModel.createCapsule(us, vs, radius0, halfHeight)
        transformYToAxis(mesh, Axis.X)
        val sdf = SDFCapsule().apply {
            radius = radius0
            p0.set(-halfHeight, 0f, 0f)
            p1.set(+halfHeight, 0f, 0f)
        }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -(halfHeight + radius0), -radius0, -radius0,
                +(halfHeight + radius0), +radius0, +radius0
            ), 1e-3f
        )
    }

    @Test
    fun testCapsuleY() {
        val mesh = CapsuleModel.createCapsule(us, vs, radius0, halfHeight)
        val sdf = SDFCapsule().apply {
            radius = radius0
            p0.set(0f, -halfHeight, 0f)
            p1.set(0f, +halfHeight, 0f)
        }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -radius0, -(halfHeight + radius0), -radius0,
                +radius0, +(halfHeight + radius0), +radius0
            ), 1e-3f
        )
    }

    @Test
    fun testCapsuleZ() {
        val mesh = CapsuleModel.createCapsule(us, vs, radius0, halfHeight)
        transformYToAxis(mesh, Axis.Z)
        val sdf = SDFCapsule().apply {
            radius = radius0
            p0.set(0f, 0f, -halfHeight)
            p1.set(0f, 0f, +halfHeight)
        }
        assertMeshEqualsSDF(
            mesh, sdf, 3e-7f, AABBf(
                -radius0, -radius0, -(halfHeight + radius0),
                +radius0, +radius0, +(halfHeight + radius0)
            ), 1e-3f
        )
    }
}