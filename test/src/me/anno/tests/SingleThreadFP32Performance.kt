package me.anno.tests

import me.anno.maths.Maths.PIf

fun main() {

    val lengthBits = 10
    val totalBits = 33
    val length = 1 shl lengthBits
    val runs = 1 shl (totalBits - lengthBits)

    val x = FloatArray(length)
    val y = FloatArray(length)

    val start = System.nanoTime()

    val a = PIf
    for (i in 0 until runs) {
        for (j in 0 until length) {
            y[j] = a * x[j] + y[j]
        }
    }

    val end = System.nanoTime()
    val duration = (end - start) / 1e9
    val ops = 2.0 * runs * length
    val gFlops = ops / duration / 1e9
    println("dur: ${duration}s, GFlops: $gFlops")

}