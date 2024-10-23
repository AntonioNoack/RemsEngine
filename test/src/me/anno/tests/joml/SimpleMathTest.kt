package me.anno.tests.joml

import me.anno.maths.Maths.min
import me.anno.utils.assertions.assertEquals
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class SimpleMathTest {

    @Test
    fun testZero() {
        assertEquals(Vector2f(), Vector2f(1f, 2f).zero())
        assertEquals(Vector3f(), Vector3f(1f, 2f, 3f).zero())
        assertEquals(Vector4f(0f), Vector4f(1f, 2f, 3f, 4f).zero())
        assertEquals(Vector2d(), Vector2d(1.0, 2.0).zero())
        assertEquals(Vector3d(), Vector3d(1.0, 2.0, 3.0).zero())
        assertEquals(Vector4d(0.0), Vector4d(1.0, 2.0, 3.0, 4.0).zero())
        assertEquals(Vector2i(), Vector2i(1, 2).zero())
        assertEquals(Vector3i(), Vector3i(1, 2, 3).zero())
        assertEquals(Vector4i(0), Vector4i(1, 2, 3, 4).zero())
    }

    @Test
    fun testNegation() {
        testUnaryOperation("negate") { -it }
    }

    @Test
    fun testAbsolute() {
        testUnaryOperation("absolute") { abs(it) }
    }

    @Test
    fun testAddition() {
        testBinaryOperation("add") { a, b -> a + b }
    }

    @Test
    fun testSubtraction() {
        testBinaryOperation("sub") { a, b -> a - b }
    }

    @Test
    fun testMultiplication() {
        testBinaryOperation("mul") { a, b -> a * b }
    }

    @Test
    fun testDivision() {
        testBinaryOperation("div") { a, b -> a / b }
    }

    @Test
    fun testMin() {
        testBinaryOperation("min") { a, b -> min(a, b) }
    }

    @Test
    fun testMax() {
        testBinaryOperation("max") { a, b -> max(a, b) }
    }

    fun testUnaryOperation(functionName: String, expectedFunc: (Double) -> Double) {
        testUnaryOperation(::Vector2f, functionName, expectedFunc)
        testUnaryOperation(::Vector3f, functionName, expectedFunc)
        testUnaryOperation(::Vector4f, functionName, expectedFunc)
        testUnaryOperation(::Vector2d, functionName, expectedFunc)
        testUnaryOperation(::Vector3d, functionName, expectedFunc)
        testUnaryOperation(::Vector4d, functionName, expectedFunc)
        testUnaryOperation(::Vector2i, functionName, expectedFunc)
        testUnaryOperation(::Vector3i, functionName, expectedFunc)
        testUnaryOperation(::Vector4i, functionName, expectedFunc)
    }

    fun testBinaryOperation(functionName: String, expectedFunc: (Double, Double) -> Double) {
        testBinaryOperation(::Vector2f, functionName, expectedFunc)
        testBinaryOperation(::Vector3f, functionName, expectedFunc)
        testBinaryOperation(::Vector4f, functionName, expectedFunc)
        testBinaryOperation(::Vector2d, functionName, expectedFunc)
        testBinaryOperation(::Vector3d, functionName, expectedFunc)
        testBinaryOperation(::Vector4d, functionName, expectedFunc)
        testBinaryOperation(::Vector2i, functionName, expectedFunc)
        testBinaryOperation(::Vector3i, functionName, expectedFunc)
        testBinaryOperation(::Vector4i, functionName, expectedFunc)
    }

    fun <V : Vector> testBinaryOperation(
        createVector: () -> V, functionName: String,
        expectedFunc: (Double, Double) -> Double
    ) {
        val a = createVector()
        val b = createVector()
        val expected = createVector()
        val random = Random(1234L)
        val method = a::class.java
            .getMethod(functionName, a::class.java, a::class.java)
        for (j in 0 until 1000) {
            for (i in 0 until a.numComponents) {
                a.setComp(i, (random.nextDouble() - 0.5) * 20.0)
                b.setComp(i, (random.nextDouble() - 0.5) * 20.0)
                if (b.getComp(i) == 0.0 && functionName == "div") {
                    b.setComp(i, 1.0) // division by zero shall be skipped
                }
                expected.setComp(i, expectedFunc(a.getComp(i), b.getComp(i)))
            }
            val result = method.invoke(a, b, createVector())
            assertEquals(expected, result)
        }
    }

    fun <V : Vector> testUnaryOperation(
        createVector: () -> V, functionName: String,
        expectedFunc: (Double) -> Double
    ) {
        val a = createVector()
        val expected = createVector()
        val random = Random(1234L)
        for (i in 0 until a.numComponents) {
            a.setComp(i, (random.nextDouble() - 10.0) * 20.0)
            expected.setComp(i, expectedFunc(a.getComp(i)))
        }
        val result = a::class.java
            .getMethod(functionName, a::class.java)
            .invoke(a, createVector())
        assertEquals(expected, result)
    }
}