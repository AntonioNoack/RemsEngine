package me.anno.tests.utils

import me.anno.fonts.Codepoints.codepoints
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CodepointsTest {
    @Test
    fun test() {
        assertEquals(listOf(128522), "😊".codepoints().toList())
        // todo implement that this is a single one
        assertEquals(2, "\uD83C\uDDE9\uD83C\uDDEA".codepoints().size)
    }
}