package me.anno.bench.sdftexture

import me.anno.Time
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.computeDistances

/**
 * Find the slowest letter in Verdana, 20
 * */
fun main() {

    OfficialExtensions.initForTests()
    val font = Font("Verdana", 20f)

    val cp0 = 33
    val cp1 = 128

    fun generate(codepoint: Int) {
        val roundEdges = false
        computeDistances(font, codepoint, roundEdges)
    }

    generate(32)

    val timeByChar = (cp0 until cp1).map { cp ->
        val t0 = Time.nanoTime
        generate(cp)
        val dt = Time.nanoTime - t0
        cp to dt
    }.sortedByDescending { it.second }

    val slowestChars = timeByChar.subList(0, 5)
    // '@', 67.6659 ms, '&', 53.1946 ms, '$', 51.0996 ms, '%', 44.0215 ms, '6', 41.6033 ms
    // '@', 64.9243 ms, '&', 51.3427 ms, '$', 48.6044 ms, '%', 42.6583 ms, '6', 42.371 ms
    // '@', 65.5522 ms, '&', 51.3118 ms, '$', 48.9258 ms, '%', 42.6766 ms, '6', 41.6863 ms
    println("Slowest chars: ${slowestChars.map { (cp, dt) -> "'${cp.toChar()}', ${(dt / 1e6f)} ms" }}")
}