package me.anno.bench

import me.anno.Engine
import me.anno.engine.ui.render.Frustum
import me.anno.utils.Clock
import org.joml.AABBd
import org.joml.Matrix4d
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {

    // compare Matrix4f.testAab() to Frustum()-class
    // -> which is faster?
    // -> our Frustum class is 1.5x faster
    // -> our Frustum class is still 16% faster than MatrixFrustum

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
    clock.benchmark(10, 5000, cubes.size, "Frustum") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            frustum.contains(cube)
        }
    }

    val matrix = Matrix4d()
    matrix.setPerspective(1.0, 1.0, 1e-3, 1e6)
    clock.benchmark(10, 5000, cubes.size, "Matrix") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            matrix.testAab(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ)
        }
    }

    val matrixFrustum = MatrixFrustum(matrix)
    clock.benchmark(10, 5000, cubes.size, "MatrixFrustum") {
        for (ci in cubes.indices) {
            val cube = cubes[ci]
            matrixFrustum.testAab(cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY, cube.maxZ)
        }
    }
}

class MatrixFrustum(m: Matrix4d) {

    val nxX = m.m03 + m.m00
    val nxY = m.m13 + m.m10
    val nxZ = m.m23 + m.m20
    val nxW = m.m33 + m.m30
    val pxX = m.m03 - m.m00
    val pxY = m.m13 - m.m10
    val pxZ = m.m23 - m.m20
    val pxW = m.m33 - m.m30
    val nyX = m.m03 + m.m01
    val nyY = m.m13 + m.m11
    val nyZ = m.m23 + m.m21
    val nyW = m.m33 + m.m31
    val pyX = m.m03 - m.m01
    val pyY = m.m13 - m.m11
    val pyZ = m.m23 - m.m21
    val pyW = m.m33 - m.m31
    val nzX = m.m03 + m.m02
    val nzY = m.m13 + m.m12
    val nzZ = m.m23 + m.m22
    val nzW = m.m33 + m.m32
    val pzX = m.m03 - m.m02
    val pzY = m.m13 - m.m12
    val pzZ = m.m23 - m.m22
    val pzW = m.m33 - m.m32

    fun testAab(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return nxX * (if (nxX < 0.0) minX else maxX) + nxY * (if (nxY < 0.0) minY else maxY) + nxZ * (if (nxZ < 0.0) minZ else maxZ) >= -nxW &&
                pxX * (if (pxX < 0.0) minX else maxX) + pxY * (if (pxY < 0.0) minY else maxY) + pxZ * (if (pxZ < 0.0) minZ else maxZ) >= -pxW &&
                nyX * (if (nyX < 0.0) minX else maxX) + nyY * (if (nyY < 0.0) minY else maxY) + nyZ * (if (nyZ < 0.0) minZ else maxZ) >= -nyW &&
                pyX * (if (pyX < 0.0) minX else maxX) + pyY * (if (pyY < 0.0) minY else maxY) + pyZ * (if (pyZ < 0.0) minZ else maxZ) >= -pyW &&
                nzX * (if (nzX < 0.0) minX else maxX) + nzY * (if (nzY < 0.0) minY else maxY) + nzZ * (if (nzZ < 0.0) minZ else maxZ) >= -nzW &&
                pzX * (if (pzX < 0.0) minX else maxX) + pzY * (if (pzY < 0.0) minY else maxY) + pzZ * (if (pzZ < 0.0) minZ else maxZ) >= -pzW
    }
}