package me.anno.utils

import org.joml.Vector4f
import kotlin.math.abs

typealias Color = Vector4f

fun Int.r() = shr(16) and 255
fun Int.g() = shr(8) and 255
fun Int.b() = this and 255
fun Int.a() = shr(24) and 255

fun rgba(r: Int, g: Int, b: Int, a: Int) = clamp(r, 0, 255).shl(16) or
        clamp(g, 0, 255).shl(8) or
        clamp(b, 0, 255) or
        clamp(a, 0, 255).shl(24)

fun colorDifference(c0: Int, c1: Int): Int {
    val dr = abs(c0.r() - c1.r())
    val dg = abs(c0.g() - c1.g())
    val db = abs(c0.b() - c1.b())
    return dr+dg+db
}

