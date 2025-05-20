package me.anno.tests.audio

import me.anno.maths.noise.PerlinNoise

/**
 * a test to generate wind noise xD
 * */
fun main() {
    val fastNoise = PerlinNoise(1234L, 6, 0.5f, -1f, +1f)
    val modulator = PerlinNoise(1234L, 3, 0.5f, 0.3f, +1f)
    testProcedural {
        val time = it.toFloat()
        (fastNoise[time * 200f] * modulator[time]).toDouble()
    }
}