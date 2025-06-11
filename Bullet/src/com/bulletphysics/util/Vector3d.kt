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

fun Vector3d.setCross(var1: Vector3d, var2: Vector3d) {
    val var3 = var1.y * var2.z - var1.z * var2.y
    val var5 = var2.x * var1.z - var2.z * var1.x
    this.z = var1.x * var2.y - var1.y * var2.x
    this.x = var3
    this.y = var5
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
