package me.anno.tests.maths.chunks.spherical

import me.anno.maths.Maths.PHI
import me.anno.maths.optimization.GradientDescent
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import kotlin.math.abs

fun main() {
    // 0, ±1, ±φ
    val phi = PHI
    // (-0.5257301706705754,0.0,0.8506513901985276)
    val v0 = Vector3d(1.0, 0.0, phi).normalize()
    val v1 = Vector3d(1.0, 0.0, -phi).normalize()
    println(v0)
    println(v1)
    val tmp = Vector3d()
    val angle0 = (63.43494882292201).toRadians() / 2 // = 90-atan(4/3)/2
    println(v0.rotateY(angle0, Vector3d()))
    println(v1.rotateY(angle0, Vector3d()))
    val (err, v) = GradientDescent.simplexAlgorithm(
        doubleArrayOf(angle0), 1.0 / 1000000000000000.0, 0.0, 512
    ) {
        val a = it[0]
        v1.rotateY(a, tmp)
        println("checking ${a.toDegrees() * 2.0} -> ${tmp.x}")
        abs(tmp.x)
    }
    println("$err, ${v[0].toDegrees() * 2.0}")
    v0.rotateY(angle0)
    v1.rotateY(angle0)
    println(v0)
    println(v1)
    v0.rotateY(angle0)
    v1.rotateY(angle0)
    println(v0)
    println(v1)
    v0.rotateY(angle0)
    v1.rotateY(angle0)
    println(v0) // 0.8/0/0.6???
    println(v1)
    println(
        Vector3d(0.8944271909999162, 0.0, 0.44721359549995754)
            .rotateY(360.0 / 5.0)
    )
    println(
        Vector3d(0.8944271909999162, 0.0, 0.44721359549995754)
            .rotateY(360.0 * 2 / 5.0)
    )
    HexagonSphere.vertices
}