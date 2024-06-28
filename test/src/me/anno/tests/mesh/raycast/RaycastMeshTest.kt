package me.anno.tests.mesh.raycast

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.RaycastMesh
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

object RaycastMeshTest {

    // todo tests for global raycasting
    // todo tests (and implementation) for animated meshes

    private val mesh = flatCube.front

    @Test
    fun testSpecialCases() {
        checkLocalHit(
            mesh, Vector3f(), Vector3f(0f, 0f, 1f), 1e9f,
            0, Float.POSITIVE_INFINITY, Vector3f()
        ) // type 0 -> no hit
        checkLocalHit(
            mesh, Vector3f(), Vector3f(0f, 0f, 1f), 0f,
            -1, Float.POSITIVE_INFINITY, Vector3f()
        ) // zero distance -> no hit
        checkLocalHit(
            mesh, Vector3f(), Vector3f(0f, 0f, 1f), -1f,
            -1, Float.POSITIVE_INFINITY, Vector3f()
        ) // negative distance -> no hit
    }

    @Test
    fun testCulling() {
        checkCulling(flatCube.front, expectedFrontHit = true, expectedBackHit = false)
        checkCulling(flatCube.back, expectedFrontHit = false, expectedBackHit = true)
        checkCulling(flatCube.both, expectedFrontHit = true, expectedBackHit = true)
    }

    fun checkCulling(mesh: Mesh, expectedFrontHit: Boolean, expectedBackHit: Boolean) {
        val dist0 = 2f
        val dist1 = 4f
        val norm0 = Vector3f(0f, 0f, -1f)
        val norm1 = Vector3f(0f, 0f, +1f)
        val pos = Vector3f(0f, 0f, -3f)
        val dir = Vector3f(0f, 0f, 1f)
        checkLocalHit(
            mesh, pos, dir, 6f,
            Raycast.TRIANGLE_FRONT, if (expectedFrontHit) dist0 else dist1, if (expectedFrontHit) norm0 else norm1
        ) // from front
        checkLocalHit(
            mesh, pos, dir, 6f,
            Raycast.TRIANGLE_BACK, if (expectedBackHit) dist0 else dist1, if (expectedBackHit) norm0 else norm1
        ) // from back
        checkLocalHit(
            mesh, pos, dir, 6f,
            -1, dist0, norm0
        ) // all sides -> expect a hit
    }

    @Test
    fun testEdgeCasesInside() {
        val positions = mesh.positions!!
        val a = Vector3f()
        val b = Vector3f()
        mesh.forEachLineIndex { ai, bi ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            val dir = a.lerp(b, 0.5f)
            val pos = b.set(0f)
            checkLocalHit(mesh, pos, dir, 2f, -1, 1f, null)
        }
    }

    @Test
    fun testEdgeCasesOutside() {
        val positions = mesh.positions!!
        val a = Vector3f()
        val b = Vector3f()
        mesh.forEachLineIndex { ai, bi ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            val dir = a.lerp(b, 0.5f)
            val pos = a.mul(-3f, b)
            checkLocalHit(mesh, pos, dir, 3f, -1, 2f, null)
        }
    }

    @Test
    fun testCornerCasesInside() {
        val pos = Vector3f()
        val dir = Vector3f()
        mesh.forEachPoint(false) { x, y, z ->
            dir.set(x, y, z)
            checkLocalHit(mesh, pos, dir, 2f, -1, 1f, null)
        }
    }

    @Test
    fun testCornerCasesOutside() {
        val pos = Vector3f()
        val dir = Vector3f()
        mesh.forEachPoint(false) { x, y, z ->
            pos.set(x, y, z).mul(-3f)
            dir.set(x, y, z)
            checkLocalHit(mesh, pos, dir, 3f, -1, 2f, null)
        }
    }

    fun checkLocalHit(
        mesh: Mesh, pos: Vector3f, dir: Vector3f, maxDistance: Float, typeMask: Int,
        expectedDistance: Float,
        expectedNormal: Vector3f?
    ) {

        val normal = Vector3f()
        val distance = RaycastMesh.raycastLocalMeshClosestHit(mesh, pos, dir, maxDistance, typeMask, normal)
        if (expectedNormal != null) assertEquals(expectedNormal, normal)
        assertEquals(expectedDistance, distance, 1e-3f)

        val hit = RaycastMesh.raycastLocalMeshAnyHit(mesh, pos, dir, maxDistance, typeMask)
        assertEquals(distance.isFinite(), hit)
    }
}