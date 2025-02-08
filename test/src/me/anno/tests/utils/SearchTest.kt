package me.anno.tests.utils

import me.anno.ui.base.Search
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchTest {
    @Test
    fun testSimpleNegation() {
        val search = Search("!.png")
        assertFalse(search.matches("nemo.png"))
        assertTrue(search.matches("nemo.jpg"))
    }

    @Test
    fun testExplicitAnd() {
        val search = Search("a & b")
        assertFalse(search.matches(""))
        assertFalse(search.matches("a"))
        assertFalse(search.matches("b"))
        assertTrue(search.matches("ab"))
        assertTrue(search.matches("ba"))
    }

    @Test
    fun testImplicitAnd() {
        val search = Search("a b")
        assertFalse(search.matches(""))
        assertFalse(search.matches("a"))
        assertFalse(search.matches("b"))
        assertTrue(search.matches("ab"))
        assertTrue(search.matches("ba"))
    }

    @Test
    fun testOr() {
        val search = Search("a | b")
        assertFalse(search.matches(""))
        assertTrue(search.matches("a"))
        assertTrue(search.matches("b"))
        assertTrue(search.matches("ab"))
        assertTrue(search.matches("ba"))
    }

    @Test
    fun testComplexOr() {
        val search = Search("!(!a & !b)")
        assertFalse(search.matches(""))
        assertTrue(search.matches("a"))
        assertTrue(search.matches("b"))
        assertTrue(search.matches("ab"))
        assertTrue(search.matches("ba"))
    }

    @Test
    fun testComplexAnd() {
        val search = Search("!(!a | !b)")
        assertFalse(search.matches(""))
        assertFalse(search.matches("a"))
        assertFalse(search.matches("b"))
        assertTrue(search.matches("ab"))
        assertTrue(search.matches("ba"))
    }

    @Test
    fun testLeadingSymbols() {
        val search = Search("&|a")
        assertFalse(search.matches("b"))
        assertTrue(search.matches("a"))
    }

    @Test
    fun testTrailingSymbols() {
        val search = Search("a&|!")
        assertFalse(search.matches("b"))
        assertTrue(search.matches("a"))
    }

    @Test
    fun testQuotesWithSpecialSymbols() {
        val search = Search("\"a | b\"")
        assertTrue(search.matches("a | b"))
        assertFalse(search.matches("a"))
        assertFalse(search.matches("b"))
    }

    @Test
    fun testQuickSearch() {
        assertTrue(Search("wg").matches("WolfGang"))
        assertTrue(Search("ab").matches("Alpha Bar"))
        assertTrue(Search("aa").matches("Alpha Angst"))
        assertFalse(Search("wg").matches("wolfgang"))
    }

    @Test
    fun testQuickSearchMissingOneChar() {
        assertTrue(Search("wg").matches("wog"))
        assertFalse(Search("wg").matches("woog"))
    }
}