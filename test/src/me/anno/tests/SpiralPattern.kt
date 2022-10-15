package me.anno.tests

import me.anno.image.ImageWriter
import me.anno.maths.patterns.SpiralPattern
import org.joml.Vector3i

fun main() {
    val width = 256
    val radius = 10
    val size = 2 * radius + 1
    val list = SpiralPattern.roundSpiral2d(radius, 0, false)
    val coordsToIndex =
        list.withIndex().associate { it.value to (255f * it.index / (list.size - 1f)).toInt() * 0x10101 }
    ImageWriter.writeImageInt(width, width, false, "spiral.png", 512) { x, y, _ ->
        val cx = (x * size / width) - radius
        val cz = (y * size / width) - radius
        coordsToIndex[Vector3i(cx, 0, cz)] ?: 0x770077
    }
}