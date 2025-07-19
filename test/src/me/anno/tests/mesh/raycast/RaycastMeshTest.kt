package me.anno.tests.mesh.raycast

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachLine
import me.anno.ecs.components.mesh.MeshIterators.forEachPoint
import me.anno.engine.raycast.BLASCache.disableBLASCache
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.raycast.RaycastMesh
import me.anno.maths.Maths.SQRT2f
import me.anno.maths.Maths.SQRT3f
import me.anno.maths.bvh.HitType
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.FlakyTest
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.min

object RaycastMeshTest {

    // todo tests for global raycasting with transform
    // todo tests (and implementation) for animated meshes

    private val mesh = flatCube.front

    @Test
    fun testSpecialCases() {
        checkHit(
            mesh, Vector3f(), Vector3f(0f, 0f, 1f), 1e9f,
            0, Float.POSITIVE_INFINITY, Vector3f()
        ) // type 0 -> no hit
        checkHit(
            mesh, Vector3f(), Vector3f(0f, 0f, 1f), 0f,
            -1, Float.POSITIVE_INFINITY, Vector3f()
        ) // zero distance -> no hit
        checkHit(
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
        checkHit(
            mesh, pos, dir, 6f,
            Raycast.TRIANGLE_FRONT, if (expectedFrontHit) dist0 else dist1, if (expectedFrontHit) norm0 else norm1
        ) // from front
        checkHit(
            mesh, pos, dir, 6f,
            Raycast.TRIANGLE_BACK, if (expectedBackHit) dist0 else dist1, if (expectedBackHit) norm0 else norm1
        ) // from back
        checkHit(
            mesh, pos, dir, 6f,
            -1, dist0, norm0
        ) // all sides -> expect a hit
    }

    @Test
    @FlakyTest
    fun testEdgeCasesInside() {
        mesh.forEachLine { a, b ->
            val dir = a.mix(b, 0.5f).normalize()
            val pos = b.set(0f)
            if (dir.absMax() > 0.95f) {
                checkHit(mesh, pos, dir, 2f, -1, 1f, null)
            }
            false
        }
    }

    @Test
    @FlakyTest
    fun testEdgeCasesOutside() {
        mesh.forEachLine { a, b ->
            val dir = a.mix(b, 0.5f).normalize()
            val pos = a.mul(-3f, b)
            if (dir.absMax() > 0.95f) {
                checkHit(mesh, pos, dir, 3f, -1, 2f, null)
            }
            false
        }
    }

    @Test
    fun testCornerCasesInside() {
        val pos = Vector3f()
        val dir = Vector3f()
        mesh.forEachPoint(false) { x, y, z ->
            dir.set(x, y, z).normalize()
            checkHit(mesh, pos, dir, 2f, -1, SQRT3f, null)
            false
        }
    }

    @Test
    @FlakyTest
    fun testCornerCasesOutside() {
        val pos = Vector3f()
        val dir = Vector3f()
        mesh.forEachPoint(false) { x, y, z ->
            pos.set(x, y, z).mul(-3f)
            dir.set(x, y, z).normalize()
            checkHit(mesh, pos, dir, 5f, -1, 2f * SQRT3f, null)
            false
        }
    }

    fun checkHit(
        mesh: Mesh, pos: Vector3f, dir: Vector3f, maxDistance: Float, typeMask: Int,
        expectedDistance: Float, expectedNormal: Vector3f?
    ) {
        disableBLASCache = true
        checkLocalHit(mesh, pos, dir, maxDistance, typeMask, expectedDistance, expectedNormal)
        checkGlobalHit(mesh, pos, dir, maxDistance, typeMask, expectedDistance, expectedNormal)
        disableBLASCache = false
    }

    fun checkLocalHit(
        mesh: Mesh, pos: Vector3f, dir: Vector3f, maxDistance: Float, typeMask: Int,
        expectedDistance: Float, expectedNormal: Vector3f?
    ) {

        val normal = Vector3f()
        val distance = RaycastMesh.raycastLocalMesh(mesh, pos, dir, maxDistance, typeMask, normal, true)
        if (expectedNormal != null) assertEquals(expectedNormal, normal)
        assertEquals(min(expectedDistance, 1e38f), min(distance, 1e38f), 1e-3f)

        val hit = RaycastMesh.raycastLocalMesh(mesh, pos, dir, maxDistance, typeMask, null, false)
        assertEquals(distance.isFinite(), hit.isFinite())
    }

    fun checkGlobalHit(
        mesh: Mesh, pos: Vector3f, dir: Vector3f, maxDistance: Float, typeMask: Int,
        expectedDistance: Float, expectedNormal: Vector3f?
    ) {

        val normal = Vector3f()
        val query = RayQuery(Vector3d(pos), Vector3f(dir), maxDistance.toDouble())
        query.typeMask = typeMask
        val transform = Transform()
        val hit = RaycastMesh.raycastGlobalMesh(query, transform, mesh)
        val distance = if (hit) query.result.distance else Double.POSITIVE_INFINITY
        normal.set(query.result.geometryNormalWS).safeNormalize()
        if (expectedNormal != null) assertEquals(expectedNormal, normal)
        assertEquals(min(expectedDistance.toDouble(), 1e38), min(distance, 1e38), 1e-3)

        val query1 = RayQuery(Vector3d(pos), Vector3f(dir), maxDistance.toDouble())
        query1.result.hitType = HitType.ANY
        query1.typeMask = typeMask
        val hit1 = RaycastMesh.raycastGlobalMesh(query1, transform, mesh)
        assertEquals(distance.isFinite(), hit1)
    }
}