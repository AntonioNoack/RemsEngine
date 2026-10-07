package me.anno.bench

import me.anno.engine.ui.render.Frustum
import me.anno.utils.Clock
import org.joml.Matrix4d
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {

    // compare Matrix4f.testSphere() to Frustum()-class
    // -> which is faster?
    // -> our Frustum class is 30x faster
    // -> our Frustum class is still 4.6x faster than MatrixFrustum

    val clock = Clock("Frustum Bench")

    class Sphere(val x: Double, val y: Double, val z: Double, val r: Double)

    val s = 1.0
    val cubes = (-10..10).flatMap { z ->
        (-10..10).flatMap { y ->
            (-10..10).map { x ->
                Sphere(x.toDouble(), y.toDouble(), z.toDouble(), s)
            }
        }
    }.shuffled()

    val frustum = Frustum()
    frustum.definePerspective(1e-3f, 1e6f, 1f, 1000, 1f, Vector3d(), Quaternionf())

    // 4.0ns/e
    clock.benchmark(1000, 50000, cubes.size, "FrustumV1") {
        for (ci in cubes.indices) {
            val sphere = cubes[ci]
            frustum.containsSphereV1(sphere.x, sphere.y, sphere.z, sphere.r)
        }
    }

    // 0.64ns/e
    clock.benchmark(1000, 50000, cubes.size, "FrustumV2") {
        for (ci in cubes.indices) {
            val sphere = cubes[ci]
            frustum.containsSphere(sphere.x, sphere.y, sphere.z, sphere.r)
        }
    }

    val matrix = Matrix4d() // 19ns/e
    matrix.setPerspective(1.0, 1.0, 1e-3, 1e6)
    clock.benchmark(1000, 50000, cubes.size, "Matrix") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            matrix.testSphere(cube.x, cube.y, cube.z, cube.r)
        }
    }

    val matrixFrustum = MatrixFrustum(matrix)// 3.0ns/e
    clock.benchmark(1000, 50000, cubes.size, "MatrixFrustum") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            matrixFrustum.testSphere(cube.x, cube.y, cube.z, cube.r)
        }
    }
}

fun Frustum.containsSphereV1(px: Double, py: Double, pz: Double, radius: Double): Boolean {
    if (radius < 0.0) return false
    for (i in 0 until numPlanes) {
        val plane = planes[i]
        val x = if (plane.dirX > 0.0) px - radius else px + radius
        val y = if (plane.dirY > 0.0) py - radius else py + radius
        val z = if (plane.dirZ > 0.0) pz - radius else pz + radius
        // outside
        if (plane.dot(x, y, z) >= 0.0) return false
    }
    return true
}
