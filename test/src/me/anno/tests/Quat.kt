package me.anno.tests

import me.anno.io.files.FileReference.Companion.getReference
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f

// a small HLSL functionality test for my master thesis

typealias float3 = Vector3f
typealias float4 = Quaternionf

fun normalize(v: Vector3f) = Vector3f(v).normalize()
fun normalize(v: Quaternionf) = Quaternionf(v).normalize()
fun dot(a: Vector2f, b: Vector2f) = a.dot(b)

val Vector3f.xy get() = Vector2f(x, y)
val Vector3f.xz get() = Vector2f(x, z)

fun cross(a: Vector3f, b: Vector3f) = Vector3f(a).cross(b)

fun normalToQuaternion(v0: float3, v1: float3, v2: float3): float4 {
    val dst: float4
    val diag: Float = v0.x + v1.y + v2.z
    dst = if (diag >= 0f) {
        float4(v1.z - v2.y, v2.x - v0.z, v0.y - v1.x, diag + 1f)
    } else if (v0.x >= v1.y && v0.x >= v2.z) {
        float4(v0.x - (v1.y + v2.z) + 1f, v1.x + v0.y, v0.z + v2.x, v1.z - v2.y)
    } else if (v1.y > v2.z) {
        float4(v1.x + v0.y, v1.y - (v2.z + v0.x) + 1f, v2.y + v1.z, v2.x - v0.z)
    } else {
        float4(v0.z + v2.x, v2.y + v1.z, v2.z - (v0.x + v1.y) + 1f, v0.y - v1.x)
    }
    return normalize(dst)
}

fun normalToQuaternion(v1: float3): float4 {
    val v0: float3 = if (dot(v1.xz, v1.xz) > 0.01) normalize(float3(v1.z, 0f, -v1.x)) else float3(1, 0, 0)
    val v2: float3 = cross(v0, v1)
    return normalToQuaternion(v0, v1, v2)
}

fun normalToQuaternionZ(v2: float3): float4 {
    val v1: float3 = if (dot(v2.xy, v2.xy) > 0.01) normalize(float3(v2.y, -v2.x, 0f)) else float3(1, 0, 0)
    val v0: float3 = cross(v1, v2)
    return normalToQuaternion(v0, v1, v2)
}


fun main() {

    /*val epsilon = 0.1f
    val r = Random(1234)
    for (i in 0 until 100000) {
        // gen normal
        val v = Vector3f(Vector3d(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()))
        v.me.anno.tests.normalize()
        // gen quat
        val qy = me.anno.tests.normalToQuaternion(v).conjugate()
        // test alignment
        if (qy.transform(v, Vector3f()).distance(0f, 1f, 0f) > epsilon)
            throw IllegalStateException("[$i] $qy x $v = ${qy.transform(v, Vector3f())} != (0,1,0)")
        val qz = me.anno.tests.normalToQuaternionZ(v).conjugate()
        // test alignment
        if (qz.transform(v, Vector3f()).distance(0f, 0f, 1f) > epsilon)
            throw IllegalStateException("[$i] $qz x $v = ${qz.transform(v, Vector3f())} != (0,0,1)")
    }*/

    val docs = getReference("C:/Users/Antonio/Documents/Master/GDSource/src")
    docs.findRecursively(10) {
        if (!it.isDirectory) {
            val text = it.readTextSync()
            if (".wi = " in text) {
                println(it.relativePathTo(docs, 10))
            }
        }
        false
    }

}