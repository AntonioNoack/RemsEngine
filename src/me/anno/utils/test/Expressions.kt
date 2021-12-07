package me.anno.utils.test

import me.anno.parser.SimpleExpressionParser
import kotlin.math.abs
import kotlin.math.max

fun main() {

    fun isApprox(a: Double, b: Double): Boolean {
        return abs(a - b) < 1e-5 * max(abs(a), abs(b))
    }

    fun test(expression: String, targetValue: Double) {
        val value = SimpleExpressionParser.parseDouble(expression)
        if (value == null || !isApprox(value, targetValue)) {
            throw RuntimeException("Expected $targetValue, but got $value")
        }
    }

    test("1e-10", 1e-10)
    test("3.21e-10", 3.21e-10)
    test(".1", 0.1)
    test("0.5 + .1", 0.6)
    test("1+2*3", 7.0)
    test("(1+2)*3", 9.0)
    test("(1+2)^3", 27.0)

}