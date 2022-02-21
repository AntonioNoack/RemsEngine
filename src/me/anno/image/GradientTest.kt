package me.anno.image

import kotlin.math.pow
import kotlin.math.sqrt

fun main() {
    val size = 256
    ImageWriter.writeImageFloat(size, size, "gradient.png", size, false) { x, y, _ ->
        val i = x / (size - 1f)
        when (y * 4 / size) {
            1 -> sqrt(i)
            2 -> i * i
            3 -> i.pow(2.2f)
            else -> i
        }
    }
}