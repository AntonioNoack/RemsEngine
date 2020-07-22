package me.anno.utils

import kotlin.math.abs

fun Int.r() = shr(16) and 255
fun Int.g() = shr(8) and 255
fun Int.b() = this and 255

fun colorDifference(c0: Int, c1: Int): Int {
    val dr = abs(c0.r() - c1.r())
    val dg = abs(c0.g() - c1.g())
    val db = abs(c0.b() - c1.b())
    return dr+dg+db
}