package me.anno.tests.maths.geometry

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachPointIndex
import me.anno.ecs.components.mesh.TransformMesh.transform
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.maths.geometry.MeshSplitter
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Matrix4x3
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class SplitMeshTests {

    @Test
    fun testSplitSpherePlanar() {
        testSplitSphereI(0, 20, 8, 0.15f, null) // how TF is v1 scaling???
        testSplitSphereI(1, 40, 8, 1e-7f, null)
        testSplitSphereI(2, 160, 18, 1e-7f, null)
        testSplitSphereI(3, 640, 38, 1e-7f, null)
        // testSplitSphereI(4, 2560, 78, 1e-7f, null) // four tests should be good enough ^^
    }

    @Test
    fun testSplitSphereRotated() {
        val transform = Matrix4x3().rotateX(1f)
        testSplitSphereI(0, 20, 8, 0.15f, transform)
        testSplitSphereI(1, 62, 20, 0.05f, transform)
    }

    fun testSplitSphereI(
        subDivisions: Int, expectedVerticesHalfSphere: Long, expectedVerticesHalfPlane: Long,
        allowedEpsilon: Float, transform: Matrix4x3?
    ) {
        val base = IcosahedronModel.createIcosphere(subDivisions)
        if (transform != null) base.transform(transform)
        val meshes = MeshSplitter.split(base) { it.y }
        val (top, topPlane, bottom, bottomPlane) = meshes
        checkIsHalfSphere(top, true, expectedVerticesHalfSphere, allowedEpsilon)
        checkIsHalfSphere(bottom, false, expectedVerticesHalfSphere, allowedEpsilon)
        checkIsPlane(topPlane, true, expectedVerticesHalfPlane, allowedEpsilon)
        checkIsPlane(bottomPlane, false, expectedVerticesHalfPlane, allowedEpsilon)
    }

    fun checkIsPlane(mesh: Mesh, top: Boolean, numVertices: Long, allowedEpsilon: Float) {
        // expect 18 vertices
        assertEquals(numVertices, mesh.numPrimitives)
        // all vertices-distance to center is 1
        checkVerticesAreNormalized(mesh, allowedEpsilon)
        val pos = mesh.positions!!
        val nor = mesh.normals!!
        val tmp = Vector3f()
        val expectedNormal =
            if (top) Vector3f(0f, -1f, 0f)
            else Vector3f(0f, +1f, 0f)
        mesh.forEachPointIndex(true) { i ->
            // check all vertices are on y=0,
            assertEquals(0f, tmp.set(pos, i * 3).y, 1e-6f)
            // check the normal to be 0,+/-1,0
            assertEquals(expectedNormal, tmp.set(nor, i * 3), 1e-6)
            false
        }
        // ensure the faces have the correct order by recalculating the normals
        mesh.calculateNormals(false)
        mesh.forEachPointIndex(true) { i ->
            // check all vertices are on y=0,
            assertEquals(0f, tmp.set(pos, i * 3).y, 1e-6f)
            // check the normal to be 0,+/-1,0
            assertEquals(expectedNormal, tmp.set(nor, i * 3), 1e-6)
            false
        }
    }

    fun checkIsHalfSphere(mesh: Mesh, top: Boolean, expectedNumVertices: Long, allowedEpsilon: Float) {
        // expect 160 vertices
        assertEquals(expectedNumVertices, mesh.numPrimitives)
        // check all vertices-distance to center is 1
        checkVerticesAreNormalized(mesh, allowedEpsilon)
        val pos = mesh.positions!!
        val nor = mesh.normals!!
        val tmp1 = Vector3f()
        val tmp2 = Vector3f()
        val e = 1e-7f
        mesh.forEachPointIndex(true) { i ->
            // check all vertices.y >= 0
            val py = tmp1.set(pos, i * 3).y
            if (top) assertTrue(py >= -e)
            else assertTrue(py <= e)
            // check pos = normal
            tmp2.set(nor, i * 3)
            assertEquals(tmp1, tmp2)
            false
        }
    }

    fun checkVerticesAreNormalized(mesh: Mesh, epsilon: Float = 0.02f) {
        val pos = mesh.positions!!
        val tmp = Vector3f()
        val min = 1f - epsilon
        val max = 1f + epsilon
        mesh.forEachPointIndex(true) { i ->
            tmp.set(pos, i * 3)
            assertTrue(tmp.length() in min..max) {
                "$tmp.length = ${tmp.length()} !in $min .. $max"
            }
            false
        }
        mesh.forEachPointIndex(false) { i ->
            tmp.set(pos, i * 3)
            assertTrue(tmp.length() in min..max)
            false
        }
    }
}