package me.anno.tests.utils

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Codepoints.countCodepoints
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class CodepointsTest {
    @Test
    fun test() {
        assertContentEquals(intArrayOf(128522), "😊".codepoints())
        assertContentEquals(intArrayOf(127465, 127466), "\uD83C\uDDE9\uD83C\uDDEA".codepoints())
        assertEquals(2, "\uD83C\uDDE9\uD83C\uDDEA".countCodepoints())
    }
}