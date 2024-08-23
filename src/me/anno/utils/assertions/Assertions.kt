package me.anno.utils.assertions

import me.anno.maths.Maths.sq
import org.joml.Vector
import kotlin.math.abs

fun assertTrue(condition: Boolean, message: String = "condition failed") {
    if (!condition) throw IllegalStateException(message)
}

fun assertTrue(condition: Boolean, message: () -> String) {
    if (!condition) throw IllegalStateException(message())
}

fun assertContains(value: CharSequence, collection: CharSequence, message: String = "condition failed") {
    if (value !in collection) throw IllegalStateException("'$value' !in '$collection', $message")
}

fun assertContains(value: Int, collection: IntRange, message: String = "condition failed") {
    if (value !in collection) throw IllegalStateException("'$value' !in '$collection', $message")
}

fun assertNotContains(value: CharSequence, collection: CharSequence, message: String = "condition failed") {
    if (value in collection) throw IllegalStateException("'$value' in '$collection', $message")
}

fun <V> assertContains(value: V, collection: Collection<V>, message: String = "condition failed") {
    if (value !in collection) throw IllegalStateException("'$value' !in '$collection', $message")
}

fun <V> assertNotContains(value: V, collection: Collection<V>, message: String = "condition failed") {
    if (value in collection) throw IllegalStateException("'$value' in '$collection', $message")
}

fun assertFalse(condition: Boolean, message: String = "condition failed") {
    if (condition) throw IllegalStateException(message)
}

fun assertEquals(expected: Any?, actual: Any?, message: String = "expected equal values") {
    if (expected != actual) throw IllegalStateException("$message, \n'$expected' != \n'$actual'")
}

fun assertEquals(expected: Int, actual: Int, message: String = "expected equal values") {
    if (expected != actual) throw IllegalStateException("$message, $expected != $actual")
}

fun assertEquals(
    expected: Double, actual: Double, absoluteThreshold: Double,
    message: String = "expected equal values"
) {
    if (!(abs(expected - actual) <= absoluteThreshold)) {
        throw IllegalStateException("$message, |$expected - $actual| > $absoluteThreshold")
    }
}

fun assertNotEquals(forbidden: Any?, actual: Any?, message: String = "expected different values") {
    if (forbidden == actual) throw IllegalStateException(message)
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
