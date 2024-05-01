package me.anno.utils.assertions

fun assertTrue(condition: Boolean, message: String = "condition failed") {
    if (!condition) throw IllegalStateException(message)
}

fun assertFalse(condition: Boolean, message: String = "condition failed") {
    if (condition) throw IllegalStateException(message)
}

fun assertEquals(a: Any?, b: Any?, message: String = "expected equal values") {
    if (a != b) throw IllegalStateException(message)
}

fun assertNotEquals(a: Any?, b: Any?, message: String = "expected different values") {
    if (a == b) throw IllegalStateException(message)
}