package me.anno.tests.maths

import me.anno.parser.SimpleExpressionParser
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.max

class SimpleExpressionParserTest {

    private fun isApprox(a: Double, b: Double): Boolean {
        return a == b || abs(a - b) < 1e-5 * max(abs(a), abs(b))
    }

    private fun test(expression: String, targetValue: Double) {
        val value = SimpleExpressionParser.parseDouble(expression)
        assertNotNull(value)
        assertTrue(isApprox(value!!, targetValue), "Expected $targetValue, but got $value")
    }

    @Test
    fun testNumberParsing() {
        test("1e-10", 1e-10)
        test("3.21e-10", 3.21e-10)
        test(".1", 0.1)
    }

    @Test
    fun testWithSpaces() {
        test("0.5 + .1", 0.6)
    }

    @Test
    fun testPow() {
        test("2^3", 8.0)
    }

    @Test
    fun testOperationOrder() {
        test("1+2*3", 7.0)
        test("2*3+1", 7.0)
    }

    @Test
    fun testUnaryMinus() {
        test("-1-1", -2.0)
        test("-1*-1", 1.0)
    }

    @Test
    fun testParenthesis() {
        test("(1+2)*3", 9.0)
        test("(1+2)^3", 27.0)
    }

    @Test
    fun testVectors() {
        test("[1,2,3][1]", 2.0)
        test("[1,2,3][0.5]", 1.5)
    }

    @Test
    fun testCompare() {
        test("1<1", 0.0)
        test("1<=1", 1.0)
        test("1>1", 0.0)
        test("1>=1", 1.0)
    }

    @Test
    fun testIfElse() {
        test("15 if 1 else 7", 15.0)
        test("1 ? 15 : 7", 15.0)
        test("15 if 0 else 7", 7.0)
        test("0 ? 15 : 7", 7.0)
    }

    @Test
    fun testFunctionCalls() {
        test("sin(90)", 1.0)
        test("sin(0)", 0.0)
        test("atan(0)", 0.0)
        test("atan(1)", 45.0)
        test("atan(5,5)", 45.0)
        test("atan(1/0)", 90.0)
        test("atan(+Inf)", 90.0)
        test("atan(-Inf)", -90.0)
        test("atan(-1)", -45.0)
    }
}