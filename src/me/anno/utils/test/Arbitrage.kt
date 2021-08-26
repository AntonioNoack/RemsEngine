package me.anno.utils.test

import me.anno.utils.LOGGER
import kotlin.math.ceil
import kotlin.math.sqrt

fun main() {

    val changeRates = listOf(
        0.000, 1.350, 1.351, 0.888, 1.433,
        0.741, 0.000, 1.004, 0.657, 1.061,
        0.732, 0.995, 0.000, 0.650, 1.049,
        1.126, 1.521, 1.538, 0.000, 1.614,
        0.698, 0.943, 0.953, 0.620, 0.000
    )

    val s = ceil(sqrt(changeRates.size.toFloat())).toInt()
    fun change(from: Int, to: Int) = changeRates[from * s + to]

    for (i in 0 until s) {
        for (j in 0 until s) {
            if (i != j) {
                LOGGER.info("$i -> $j -> $i, ${change(i, j) * change(j, i)}")
            }
        }
    }

}