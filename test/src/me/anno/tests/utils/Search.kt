package me.anno.tests.utils

import me.anno.ui.editor.files.Search
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun main() {
    val s = Search("!.png")
    assertFalse(s.matches("nemo.png"))
    assertTrue(s.matches("nemo.jpg"))
}