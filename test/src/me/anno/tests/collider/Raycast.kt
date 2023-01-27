package me.anno.tests.collider

import me.anno.maths.Maths
import org.joml.AABBd
import org.joml.Vector3d

fun main() {
    simpleTest()
    precisionTest()
}

fun simpleTest() {

    val y = 1.0
    val z = 1.0

    val f = 1e-3

    val start = Vector3d(-1e3, y, z)
    val dir = Vector3d(1.0, 0.0, 0.0)

    val aabb = AABBd()


    for (i in 0 until 1000) {

        val x = Maths.random()

        aabb.clear()
        aabb.union(x * (1 - f), y * (1 - f), z * (1 - f))
        aabb.union(x * (1 + f), y * (1 + f), z * (1 + f))

        val result = aabb.testLine(start, dir, 2e3)
        if (!result) throw RuntimeException("$start + t * $dir does not intersect ${aabb.print()}")

    }

    println("Finished simple test")

}

fun precisionTest() {

    val y = 1.0
    val z = 1.0

    val f = 0.1

    val start = Vector3d(-1e20, y, z)
    val dir = Vector3d(1.0, 0.0, 0.0)

    val aabb = AABBd()

    for (i in 0 until 1000) {

        val x = Maths.random()

        aabb.clear()
        aabb.union(x * (1 - f), y * (1 - f), z * (1 - f))
        aabb.union(x * (1 + f), y * (1 + f), z * (1 + f))

        val result = aabb.testLine(start, dir, 2e20)
        if (!result) throw RuntimeException("$start + t * $dir does not intersect ${aabb.print()}")

    }

    println("Finished precision test")

}