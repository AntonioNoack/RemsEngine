package me.anno.bench

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.raycast.BLASCache
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.RaycastMesh
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.Clock
import me.anno.utils.assertions.assertNull
import org.joml.Matrix4x3
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

fun main() {

    val query = RayQuery(Vector3d(0f, 0f, -5f), Vector3f(0f, 0f, 1f), 1e3)
    val transform = Matrix4x3()
        .translate(0.1, 0.2, 0.3)
        .rotateXYZ(0.1f, 0.2f, 0.3f)
        .scale(0.9f, 1.0f, 1.1f)

    // create list of meshes with increasing number of triangles
    val meshes = listOf(plane, flatCube) + (0 until 3).map {
        IcosahedronModel.createIcosphere(it)
    }

    // initialize BVHBuilder.static
    BVHBuilder.buildBLAS(flatCube, SplitMethod.MEDIAN_APPROX, 16)

    val clock = Clock("RaycastGlobalVSLocal")
    for (mesh in meshes) {
        val n = mesh.numPrimitives
        val numRuns = max(10_000_000 / n, 1_000).toInt()

        BLASCache.disableBLASCache = true
        mesh.raycaster = null // just in case, shouldn't be necessary

        clock.benchmark(
            50, numRuns, n,
            "raycastGlobalImpl[$n]"
        ) {
            query.result.distance = 1e3
            RaycastMesh.raycastGlobalImpl(query, transform, mesh)
        }

        clock.benchmark(
            50, numRuns, n,
            "raycastGlobalViaLocal[$n]"
        ) {
            query.result.distance = 1e3
            RaycastMesh.raycastGlobalViaLocal(query, transform, mesh)
        }

        assertNull(mesh.raycaster, "Raycaster should be null")

        clock.start()
        BLASCache.disableBLASCache = false
        mesh.raycaster = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
        clock.stop("Building BLAS for $n")

        clock.benchmark(
            50, numRuns, n,
            "raycastBLAS[$n]"
        ) {
            query.result.distance = 1e3
            RaycastMesh.raycastGlobalViaLocal(query, transform, mesh)
        }
    }
}