package me.anno.tests.engine.ui

import me.anno.bench.containsV1
import me.anno.engine.ui.render.Frustum
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBd
import org.joml.Quaternionf
import org.joml.Vector3d

fun main() {
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

    println(frustum.planes.toList())
    println(frustum.planesF.toList())
    println(frustum.planesD.toList())
    println(frustum.planesS)

    val matrix = IntArray(4)
    for (i in cubes.indices) {
        val cube = cubes[i]

        val actual = frustum.contains(cube)
        val expected = frustum.containsV1(cube)
        matrix[actual.toInt() + expected.toInt(2)]++
    }

    println("Matrix: ${matrix.toList()}")

}