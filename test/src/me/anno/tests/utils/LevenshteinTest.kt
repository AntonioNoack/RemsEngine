package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Strings.distance
import org.junit.jupiter.api.Test

class LevenshteinTest {
    @Test
    fun test() {
        assertEquals(3, "abc".distance("abcdef"))
        assertEquals(3, "abcdef".distance("abc"))
        assertEquals(2, "bcd".distance("abc"))
    }
}