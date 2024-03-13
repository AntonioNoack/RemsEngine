package me.anno.tests.utils

import me.anno.ui.editor.files.Search
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTest {
    @Test
    fun test() {
        val s = Search("!.png")
        assertFalse(s.matches("nemo.png"))
        assertTrue(s.matches("nemo.jpg"))
    }
}