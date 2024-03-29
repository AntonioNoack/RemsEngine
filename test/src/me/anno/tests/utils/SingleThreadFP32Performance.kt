package me.anno.tests.utils

import me.anno.Time
import me.anno.maths.Maths.PIf

fun main() {

    val lengthBits = 10
    val totalBits = 33
    val length = 1 shl lengthBits
    val runs = 1 shl (totalBits - lengthBits)

    val x = FloatArray(length)
    val y = FloatArray(length)

    val start = Time.nanoTime

    val a = PIf
    for (i in 0 until runs) {
        for (j in 0 until length) {
            y[j] = a * x[j] + y[j]
        }
    }

    val end = Time.nanoTime
    val duration = (end - start) / 1e9
    val ops = 2.0 * runs * length
    val gFlops = ops / duration / 1e9
    println("dur: ${duration}s, GFlops: $gFlops")

    // 1s, 17.3-17.6 GFlops on Ryzen 5 2600

}