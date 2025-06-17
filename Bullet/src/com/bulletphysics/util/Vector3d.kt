package com.bulletphysics.util

import org.joml.JomlMath.mix
import org.joml.Vector3d
import kotlin.math.abs

fun Vector3d.setAdd(a: Vector3d, b: Vector3d) {
    x = a.x + b.x
    y = a.y + b.y
    z = a.z + b.z
}

fun Vector3d.setSub(a: Vector3d, b: Vector3d) {
    x = a.x - b.x
    y = a.y - b.y
    z = a.z - b.z
}

fun Vector3d.setScale(s: Double, v: Vector3d) {
    x = v.x * s
    y = v.y * s
    z = v.z * s
}

fun Vector3d.setScaleAdd(a: Double, b: Vector3d, c: Vector3d) {
    x = b.x * a + c.x
    y = b.y * a + c.y
    z = b.z * a + c.z
}

fun Vector3d.setNegate(src: Vector3d) {
    x = -src.x
    y = -src.y
    z = -src.z
}

fun Vector3d.setCross(a: Vector3d, b: Vector3d) {
    val nx = a.y * b.z - a.z * b.y
    val ny = b.x * a.z - b.z * a.x
    this.z = a.x * b.y - a.y * b.x
    this.x = nx
    this.y = ny
}

fun Vector3d.setCrossY(a: Vector3d) {
    set(-a.z, 0.0, a.x)
}

fun Vector3d.setCrossZ(a: Vector3d) {
    set(a.y, -a.x, 0.0)
}

fun Vector3d.setNormalize(v: Vector3d) {
    set(v)
    normalize()
}

fun Vector3d.setInterpolate(a: Vector3d, b: Vector3d, f: Double) {
    x = mix(a.x, b.x, f)
    y = mix(a.y, b.y, f)
    z = mix(a.z, b.z, f)
}

fun Vector3d.setAbsolute(src: Vector3d) {
    x = abs(src.x)
    y = abs(src.y)
    z = abs(src.z)
}

fun Vector3d.mul(s: Double) {
    mul(s)
}
