package me.anno.tests.audio

import me.anno.maths.noise.PerlinNoise

/**
 * a test to generate wind noise xD
 * */
fun main() {
    val noise = PerlinNoise(1234L, 6, 0.5f, -1f, +1f)
    testProcedural { noise[it.toFloat() * 200f].toDouble() }
}