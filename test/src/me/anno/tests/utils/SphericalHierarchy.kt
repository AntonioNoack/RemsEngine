package me.anno.tests.utils

import me.anno.ecs.components.chunks.spherical.SphereTriangle
import org.joml.Vector3d

fun main() {
    val a = Vector3d(-1.0, -1.0, 1.0)
    val b = Vector3d(-1.0, +1.0, 1.0)
    val c = Vector3d(+2.0, +0.0, 1.0)
    val tri = SphereTriangle(null, 0, 0, a, b, c)
    println(tri.baseAB)
    println(tri.baseUp)
    println(tri.baseAC)
    println(tri.globalToLocal)
    println(tri.localToGlobal)
    println(
        tri.globalToLocal.transformPosition(Vector3d(0.0, 0.0, 1.1))
    ) // shall become (0,0.1,0)
    println(
        tri.globalToLocal.transformPosition(Vector3d(0.0, 1.0, 1.1))
    ) // shall become (1,0.1,0)
    println(tri.localA)
    println(tri.localB)
    println(tri.localC)
}