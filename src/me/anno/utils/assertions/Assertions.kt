package me.anno.utils.assertions

fun assertTrue(condition: Boolean, message: String = "condition failed") {
    if (!condition) throw IllegalStateException(message)
}

fun assertFalse(condition: Boolean, message: String = "condition failed") {
    if (condition) throw IllegalStateException(message)
}

fun assertEquals(expected: Any?, actual: Any?, message: String = "expected equal values") {
    if (expected != actual) throw IllegalStateException("$message, '$expected' != '$actual'")
}

fun assertEquals(expected: Int, actual: Int, message: String = "expected equal values") {
    if (expected != actual) throw IllegalStateException("$message, $expected != $actual")
}

fun assertNotEquals(forbidden: Any?, actual: Any?, message: String = "expected different values") {
    if (forbidden == actual) throw IllegalStateException(message)
}