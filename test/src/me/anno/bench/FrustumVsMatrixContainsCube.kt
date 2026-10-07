package me.anno.bench

import me.anno.engine.ui.render.Frustum
import me.anno.utils.Clock
import org.joml.AABBd
import org.joml.Matrix4d
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {

    // compare Matrix4f.testAab() to Frustum()-class
    // -> which is faster?
    // -> our Frustum class is 17x faster
    // -> our Frustum class is 13x faster than MatrixFrustum

    val clock = Clock("Frustum Bench")

    val s = 1.0
    val cubes = (-10..10).flatMap { z ->
        (-10..10).flatMap { y ->
            (-10..10).map { x ->
                AABBd(
                    x - s, y - s, z - s,
                    x + s, y + s, z + s
                )
            }
        }
    }.shuffled()

    val frustum = Frustum()
    frustum.definePerspective(1e-3f, 1e6f, 1f, 1000, 1f, Vector3d(), Quaternionf())

    // 4.1ns/e
    clock.benchmark(1000, 50000, cubes.size, "FrustumV1") {
        for (ci in cubes.indices) {
            frustum.containsV1(cubes[ci])
        }
    }

    // 1.05ns/e
    clock.benchmark(1000, 50000, cubes.size, "FrustumV2") {
        for (ci in cubes.indices) {
            frustum.containsV2(cubes[ci])
        }
    }

    // 0.44ns/e
    clock.benchmark(1000, 50000, cubes.size, "FrustumV3") {
        for (ci in cubes.indices) {
            frustum.contains(cubes[ci])
        }
    }

    val matrix = Matrix4d() // 7.6ns/e
    matrix.setPerspective(1.0, 1.0, 1e-3, 1e6)
    clock.benchmark(1000, 50000, cubes.size, "Matrix") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            matrix.testAab(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ)
        }
    }

    val matrixFrustum = MatrixFrustum(matrix)// 5.8ns/e
    clock.benchmark(1000, 50000, cubes.size, "MatrixFrustum") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            matrixFrustum.testAab(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ)
        }
    }
}

fun Frustum.containsV1(aabb: AABBd): Boolean {
    if (aabb.isEmpty()) return false
    // https://www.gamedev.net/forums/topic/512123-fast--and-correct-frustum---aabb-intersection/
    for (i in 0 until numPlanes) {
        val plane = planes[i]
        val x = if (plane.dirX > 0.0) aabb.minX else aabb.maxX
        val y = if (plane.dirY > 0.0) aabb.minY else aabb.maxY
        val z = if (plane.dirZ > 0.0) aabb.minZ else aabb.maxZ
        // outside
        if (plane.dot(x, y, z) >= 0.0) return false
    }
    return true
}

/**
 * algorithm from https://www.gamedev.net/forums/topic/512123-fast--and-correct-frustum---aabb-intersection/
 * just slightly optimized
 * */
fun Frustum.containsV2(aabb: AABBd): Boolean {
    val planes = planesD
    var i = numPlanes * 3
    val minX = aabb.minX
    val minY = aabb.minY
    val minZ = aabb.minZ
    val maxX = aabb.maxX
    val maxY = aabb.maxY
    val maxZ = aabb.maxZ
    while (i > 0) {
        i -= 3
        val pdx = planes[i]
        val pdy = planes[i + 1]
        val pdz = planes[i + 2]
        val x = if (pdx > 0.0) minX else maxX
        val y = if (pdy > 0.0) minY else maxY
        val z = if (pdz > 0.0) minZ else maxZ
        // outside
        if (pdx * x + pdy * y + pdz * z >= 0.0) return false
    }
    return true
}
