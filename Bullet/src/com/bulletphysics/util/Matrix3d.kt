package com.bulletphysics.util

import org.joml.Matrix3d
import org.joml.Vector3d

fun Matrix3d.setMul(a: Matrix3d, b: Matrix3d) {
    a.mul(b, this)
}

fun Matrix3d.setTranspose(src: Matrix3d) {
    src.transpose(this)
}

fun Matrix3d.setElement(i: Int, j: Int, v: Double) {
    // first number is fast on both implementations;
    // javax has stuff flipped -> flip it
    this[j, i] = v
}

fun Matrix3d.getElement(i: Int, j: Int): Double {
    // first number is fast on both implementations;
    // javax has stuff flipped -> flip it
    return this[j, i]
}

fun Matrix3d.revTransform(src: Vector3d, dst: Vector3d) {
    transform(src, dst)
}