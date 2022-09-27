package me.anno.tests.structures

import me.anno.maths.LinearRegression
import org.apache.logging.log4j.LogManager
import org.joml.Vector2d

fun main() {
    val logger = LogManager.getLogger(LinearRegression::class)
    // test polynomial of 2nd degree
    val deg2 = LinearRegression.findPolynomialCoefficients(
        listOf(
            Vector2d(-1.0, +1.0),
            Vector2d(+0.0, +0.0),
            Vector2d(+1.0, +1.0)
        )
    )!!.toList()
    logger.info("$deg2 == (0,0,1)?")
    // test polynomial of 3rd/4th degree
    val deg4 = LinearRegression.findPolynomialCoefficients(
        listOf(
            Vector2d(-2.0, +0.0),
            Vector2d(-1.0, +1.0),
            Vector2d(+0.0, +0.0),
            Vector2d(+1.0, -1.0),
            Vector2d(+2.0, +0.0)
        )
    )!!.toList()
    logger.info("$deg4 == (0,-1.333,0,0.333,0)?")
    logger.info(LinearRegression.evaluateBlackFrame(doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0)))
    logger.info(LinearRegression.evaluateBlackFrame(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0)))
}