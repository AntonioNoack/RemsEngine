package me.anno.tests.maths

import me.anno.parser.SimpleExpressionParser
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimpleExpressionParserTest {
    @Test
    fun testSimpleExpressionParser() {

        fun isApprox(a: Double, b: Double): Boolean {
            return abs(a - b) < 1e-5 * max(abs(a), abs(b))
        }

        fun test(expression: String, targetValue: Double) {
            val value = SimpleExpressionParser.parseDouble(expression)
            assertNotNull(value)
            assertTrue(isApprox(value, targetValue), "Expected $targetValue, but got $value")
        }

        test("1e-10", 1e-10)
        test("3.21e-10", 3.21e-10)
        test(".1", 0.1)
        test("0.5 + .1", 0.6)
        test("1+2*3", 7.0)
        test("(1+2)*3", 9.0)
        test("(1+2)^3", 27.0)
        test("-1-1", -2.0)
        test("-1*-1", 1.0)
    }
}