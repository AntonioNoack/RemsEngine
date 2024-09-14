package me.anno.utils.assertions

import me.anno.maths.Maths.sq
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
    assertTrue(value in collection) { "'$value' !in '$collection', $message" }
}

fun <V> assertNotContains(value: V, collection: Collection<V>, message: String = "condition failed") {
    assertTrue(value !in collection) { "'$value' in '$collection', $message" }
}

fun assertFalse(condition: Boolean, message: String = "condition failed") {
    assertTrue(!condition, message)
}

fun assertFail(message: String = "condition failed"): Nothing {
    throw IllegalStateException(message)
}

fun assertEquals(expected: Any?, actual: Any?, message: String = "expected equal values") {
    assertTrue(expected == actual) { "$message, \n  expected '$expected' != \n  actually '$actual'" }
}

fun assertEquals(expected: Any?, actual: Any?, message: () -> String) {
    assertTrue(expected == actual) { "${message()}, \n'$expected' != \n'$actual'" }
}

fun assertEquals(expected: IntArray?, actual: IntArray?, message: String = "expected equal values") {
    assertEquals(expected?.toList(), actual?.toList(), message)
}

fun assertEquals(expected: Int, actual: Int, message: String = "expected equal values") {
    assertTrue(expected == actual) { "$message, $expected != $actual" }
}

fun assertEquals(
    expected: Double, actual: Double, absoluteThreshold: Double,
    message: String = "expected equal values"
) {
    assertTrue(abs(expected - actual) <= absoluteThreshold) {
        "$message, |$expected - $actual| > $absoluteThreshold"
    }
}

fun assertEquals(
    expected: Float, actual: Float, absoluteThreshold: Float,
    message: String = "expected equal values"
) {
    assertEquals(expected.toDouble(), actual.toDouble(), absoluteThreshold.toDouble(), message)
}

fun assertNotEquals(forbidden: Any?, actual: Any?, message: String = "expected different values") {
    assertTrue(forbidden != actual) { "$message, $forbidden == $actual" }
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
    assertEquals(v, null, message)
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
    assertNotNull(instance)
    return instance!!
}
