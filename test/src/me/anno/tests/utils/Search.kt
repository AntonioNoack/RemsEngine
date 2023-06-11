package me.anno.tests.utils

import me.anno.ui.editor.files.Search

fun assert(b: Boolean) {
    if (!b) throw RuntimeException()
}

fun main() {
    val s = Search("!.png")
    assert(!s.matches("nemo.png"))
    assert(s.matches("nemo.jpg"))
}