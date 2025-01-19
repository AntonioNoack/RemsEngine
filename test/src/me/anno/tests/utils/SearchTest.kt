package me.anno.tests.utils

import me.anno.ui.editor.files.Search
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchTest {
    @Test
    fun test() {
        val s = Search("!.png")
        assertFalse(s.matches("nemo.png"))
        assertTrue(s.matches("nemo.jpg"))
    }
}