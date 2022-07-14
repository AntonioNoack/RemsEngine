package me.anno.tests

import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Random

fun main() {
    val logger = LogManager.getLogger("JOMLPrint")
    System.setProperty("joml.format", "false")
    // doesn't matter
    // System.setProperty("joml.sinLookup", "true")
    // fast-math is slower
    // System.setProperty("joml.fastmath", "true")
    val m = Matrix4f()
    val random = Random(1234)
    for (i in 0 until 16) {
        m.set(i / 4, i and 3, random.nextFloat())
    }
    val t0 = System.nanoTime()
    for (i in 0 until 1024 * 1024 * 100) {
        m.translate(0f, -1f, 0f)
        m.rotate(0.1f, 1f, 0f, 0f)
        // m.mul(m)
    }
    val t1 = System.nanoTime()
    logger.info(m)
    logger.info("${(t1 - t0) * 1e-9}s")
}