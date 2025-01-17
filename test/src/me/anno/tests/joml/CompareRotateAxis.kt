package me.anno.tests.joml

import me.anno.maths.Maths.TAU
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun main() {
    // todo there was two implementations of rotateAxis... are they equivalent??
    val random = Random(1234)
    for (i in 0 until 1000) {
        val v1 = Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble())
        val v2 = Vector3d(v1)

        val angle = random.nextDouble() * TAU
        val ax = random.nextDouble() * 2.0 - 1.0
        val ay = random.nextDouble() * 2.0 - 1.0
        val az = random.nextDouble() * 2.0 - 1.0

        val il = 1f / sqrt(ax * ax + ay * ay + az * az)

        v1.rotateAxis1(v1, angle, ax * il, ay * il, az * il)
        v2.rotateAxis2(v2, angle, ax * il, ay * il, az * il)

        assertEquals(v1, v2, 1e-9)
    }
}

fun Vector3d.rotateAxis1(dst: Vector3d, angle: Double, ax: Double, ay: Double, az: Double): Vector3d {
    val sin = sin(angle)
    val cos = cos(angle)
    val vx = x
    val vy = y
    val vz = z
    val dot = ax * vx + ay * vy + az * vz
    val invCos = 1f - cos
    return dst.set(
        vx * cos + sin * (ay * vz - az * vy) + invCos * dot * ax,
        vy * cos + sin * (az * vx - ax * vz) + invCos * dot * ay,
        vz * cos + sin * (ax * vy - ay * vx) + invCos * dot * az
    )
}

fun Vector3d.rotateAxis2(dst: Vector3d, angle: Double, ax: Double, ay: Double, az: Double): Vector3d {
    val halfAngle = angle * 0.5
    val sinAngle = sin(halfAngle)
    val qx = ax * sinAngle
    val qy = ay * sinAngle
    val qz = az * sinAngle
    val qw = cos(halfAngle)
    val w2 = qw * qw
    val x2 = qx * qx
    val y2 = qy * qy
    val z2 = qz * qz
    val zw = qz * qw
    val xy = qx * qy
    val xz = qx * qz
    val yw = qy * qw
    val yz = qy * qz
    val xw = qx * qw
    val nx = (w2 + x2 - z2 - y2) * x + (-zw + xy - zw + xy) * y + (yw + xz + xz + yw) * z
    val ny = (xy + zw + zw + xy) * x + (y2 - z2 + w2 - x2) * y + (yz + yz - xw - xw) * z
    val nz = (xz - yw + xz - yw) * x + (yz + yz + xw + xw) * y + (z2 - y2 - x2 + w2) * z
    dst.x = nx
    dst.y = ny
    dst.z = nz
    return dst
}