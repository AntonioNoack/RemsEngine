package me.anno.utils.assertions

import me.anno.maths.Maths.sq
import me.anno.utils.Logging.hash32
import me.anno.utils.types.AnyToDouble
import org.joml.Matrix
import org.joml.Vector
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

fun assertTrue(condition: Boolean, message: String = "condition failed") {
    if (!condition) assertFail(message)
}

inline fun assertTrue(condition: Boolean, message: () -> String) {
    if (!condition) assertFail(message())
}

fun <V : Comparable<V>> assertLessThan(value: V, maxValue: V, message: String = "compare failed") {
    assertTrue(value < maxValue) { "$value >= $maxValue, $message" }
}

fun <V : Comparable<V>> assertGreaterThan(value: V, maxValue: V, message: String = "compare failed") {
    assertTrue(value > maxValue) { "$value <= $maxValue, $message" }
}

fun assertContains(value: CharSequence, collection: CharSequence, message: String = "condition failed") {
    assertTrue(value in collection) { "'$value' !in '$collection', $message" }
}

fun assertContains(value: Int, collection: IntRange, message: String = "condition failed") {
    assertTrue(value in collection) { "'$value' !in '$collection', $message" }
}

fun assertNotContains(value: CharSequence, collection: CharSequence, message: String = "condition failed") {
    assertTrue(value !in collection) { "'$value' in '$collection', $message" }
}

fun <V> assertContains(value: V, collection: Collection<V>, message: String = "condition failed") {
    assertTrue(value in collection) { "${str(value)} !in '$collection', $message" }
}

fun <V> assertNotContains(value: V, collection: Collection<V>, message: String = "condition failed") {
    assertTrue(value !in collection) { "${str(value)} in '$collection', $message" }
}

fun assertFalse(condition: Boolean, message: String = "condition failed") {
    assertTrue(!condition, message)
}

fun assertFalse(condition: Boolean, message: () -> String) {
    assertTrue(!condition, message)
}

fun assertFail(message: String = "condition failed"): Nothing {
    throw IllegalStateException(message)
}

fun assertEquals(expected: Any?, actual: Any?, message: String = "expected equal values") {
    assertTrue(expected == actual) {
        "$message, \n  expected ${str(expected)} != \n  actually ${str(actual)}"
    }
}

fun str(value: Any?): String {
    return if (value != null) "'$value'" else "null"
}

fun strAt(value: Any?): String {
    return if (value != null) "'$value'@${hash32(value)}" else "null"
}

fun assertSame(expected: Any?, actual: Any?, message: String = "expected equal values") {
    assertTrue(expected === actual) {
        "$message, \n  expected ${strAt(expected)} === \n  actually ${strAt(actual)}"
    }
}

fun assertNotSame(expected: Any?, actual: Any?, message: String = "expected equal values") {
    assertTrue(expected !== actual) {
        "$message, \n  must not be the same, ${strAt(expected)}"
    }
}

fun assertEquals(expected: Any?, actual: Any?, message: () -> String) {
    assertTrue(expected == actual) { "${message()}, \n${str(expected)} != \n${str(actual)}" }
}

fun assertEquals(expected: ByteArray?, actual: ByteArray?, message: String = "expected equal values") {
    assertEquals(expected?.toList(), actual?.toList(), message)
}

fun assertEquals(expected: IntArray?, actual: IntArray?, message: String = "expected equal values") {
    assertEquals(expected?.toList(), actual?.toList(), message)
}

fun assertEquals(expected: LongArray?, actual: LongArray?, message: String = "expected equal values") {
    assertEquals(expected?.toList(), actual?.toList(), message)
}

fun assertEquals(expected: FloatArray?, actual: FloatArray?, message: String = "expected equal values") {
    assertEquals(expected?.toList(), actual?.toList(), message)
}

fun assertEquals(expected: DoubleArray?, actual: DoubleArray?, message: String = "expected equal values") {
    assertEquals(expected?.toList(), actual?.toList(), message)
}

fun assertContentEquals(expected: ByteArray?, actual: ByteArray?, message: String = "expected equal values") {
    assertEquals(expected, actual, message)
}

fun assertContentEquals(expected: IntArray?, actual: IntArray?, message: String = "expected equal values") {
    assertEquals(expected, actual, message)
}

fun assertContentEquals(expected: LongArray?, actual: LongArray?, message: String = "expected equal values") {
    assertEquals(expected, actual, message)
}

fun assertContentEquals(expected: FloatArray?, actual: FloatArray?, message: String = "expected equal values") {
    assertEquals(expected, actual, message)
}

fun assertContentEquals(expected: DoubleArray?, actual: DoubleArray?, message: String = "expected equal values") {
    assertEquals(expected, actual, message)
}

fun <V> assertContentEquals(expected: List<V>, actual: List<V>, message: String = "expected equal values") {
    assertEquals(expected, actual, message)
}

fun assertEquals(expected: Int, actual: Int, message: String = "expected equal values") {
    assertTrue(expected == actual) { "$message, $expected != $actual" }
}

fun assertEquals(expected: Char, actual: Char, message: String = "expected equal values") {
    assertTrue(expected == actual) { "$message, $expected != $actual" }
}

fun assertEquals(
    expected: Double, actual: Double, absoluteThreshold: Double,
    message: String = "expected equal values"
) {
    assertTrue(abs(expected - actual) <= absoluteThreshold) {
        "$message, |$expected - $actual| = ${abs(expected - actual)} > $absoluteThreshold"
    }
}

fun assertEquals(
    expected: Double, actual: Double, absoluteThreshold: Double,
    message: () -> String
) {
    assertTrue(abs(expected - actual) <= absoluteThreshold, message)
}

fun assertEquals(
    expected: Long, actual: Long, absoluteThreshold: Long,
    message: String = "expected equal values"
) {
    assertTrue(abs(expected - actual) <= absoluteThreshold) {
        "$message, |$expected - $actual| = ${abs(expected - actual)} > $absoluteThreshold"
    }
}

fun assertEquals(
    expected: Int, actual: Int, absoluteThreshold: Int,
    message: String = "expected equal values"
) {
    assertEquals(expected.toLong(), actual.toLong(), absoluteThreshold.toLong(), message)
}

fun assertEquals(
    expected: Float, actual: Float, absoluteThreshold: Float,
    message: String = "expected equal values"
) {
    assertEquals(expected.toDouble(), actual.toDouble(), absoluteThreshold.toDouble(), message)
}

fun <V : Vector> assertEquals(
    expected: V, actual: V, absoluteThreshold: Double,
    message: String = "expected equal values"
) {
    for (i in 0 until expected.numComponents) {
        assertEquals(expected.getComp(i), actual.getComp(i), absoluteThreshold) {
            "$message, |$expected - $actual|[$i] = ${abs(expected.getComp(i) - actual.getComp(i))} > $absoluteThreshold"
        }
    }
}

fun <M : Matrix> assertEquals(a: M, b: M, threshold: Double) {
    assertEquals(a::class, b::class)
    assertEquals(a.numRows, b.numRows)
    assertEquals(a.numCols, b.numCols)
    val clazz = a::class.java
    for (y in 0 until a.numRows) {
        for (x in 0 until a.numCols) {
            val name = "getM$x$y"
            val getter = clazz.getMethod(name)
            val fa = AnyToDouble.getDouble(getter.invoke(a))
            val fb = AnyToDouble.getDouble(getter.invoke(b))
            assertEquals(fa, fb, threshold) { "\n$a !=\n$b\n[|$fa-$fb| > $threshold, $name]" }
        }
    }
}

fun assertNotEquals(forbidden: Any?, actual: Any?, message: String = "expected different values") {
    assertTrue(forbidden != actual) { "$message, $forbidden == $actual" }
}

fun assertNotEquals(forbidden: Int, actual: Int, message: String = "expected different values") {
    assertTrue(forbidden != actual) { "$message, $forbidden == $actual" }
}

fun assertNotEquals(forbidden: Any?, actual: Any?, message: () -> String) {
    assertTrue(forbidden != actual, message)
}

fun <V : Vector> assertEquals(expected: V?, actual: V?, distanceTolerance: Double) {
    if ((expected == null) != (actual == null)) throw IllegalStateException("expected equal values, '$expected' != '$actual'")
    if (expected == null || actual == null) return
    assertEquals(expected.numComponents, actual.numComponents)
    val diffSq = (0 until expected.numComponents).sumOf {
        sq(expected.getComp(it) - actual.getComp(it))
    }
    assertTrue(diffSq < sq(distanceTolerance)) {
        "expected equal values, '$expected' != '$actual'"
    }
}

fun assertNull(v: Any?, message: String = "expected null, but got value") {
    assertEquals(null, v, message)
}

fun <V> assertNotNull(v: V?, message: String = "expected not null"): V {
    if (v == null) assertFail(message)
    return v
}

inline fun <V> assertNotNull(v: V?, message: () -> String): V {
    if (v == null) assertFail(message())
    return v
}

fun <V : Any> assertIs(expectedClass: KClass<V>, actualInstance: Any?): V {
    val instance = expectedClass.safeCast(actualInstance)
    assertNotNull(instance) { "Expected instance to be ${expectedClass.simpleName}, was ${actualInstance?.javaClass?.simpleName}" }
    return instance!!
}
