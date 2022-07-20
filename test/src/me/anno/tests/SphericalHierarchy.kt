package me.anno.tests

import me.anno.ecs.components.chunks.spherical.SphereTriangle
import me.anno.utils.types.Vectors.print
import org.joml.Vector3d

fun main() {
    val a = Vector3d(-1.0, -1.0, 1.0)
    val b = Vector3d(-1.0, +1.0, 1.0)
    val c = Vector3d(+2.0, +0.0, 1.0)
    val tri = SphereTriangle(null, 0, a, b, c)
    println(tri.baseAB.print())
    println(tri.baseUp.print())
    println(tri.baseAC.print())
    println(tri.globalToLocal)
    println(tri.localToGlobal)
    println(
        tri.globalToLocal.transformPosition(Vector3d(0.0, 0.0, 1.1)).print()
    ) // shall become (0,0.1,0)
    println(
        tri.globalToLocal.transformPosition(Vector3d(0.0, 1.0, 1.1)).print()
    ) // shall become (1,0.1,0)
    println(tri.localA.print())
    println(tri.localB.print())
    println(tri.localC.print())
}