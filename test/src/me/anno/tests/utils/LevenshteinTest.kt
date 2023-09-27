package me.anno.tests.utils

import me.anno.utils.strings.StringHelper.distance
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LevenshteinTest {
    @Test
    fun test() {
        assertEquals(3, "abc".distance("abcdef"))
        assertEquals(3, "abcdef".distance("abc"))
        assertEquals(2, "bcd".distance("abc"))
    }
}