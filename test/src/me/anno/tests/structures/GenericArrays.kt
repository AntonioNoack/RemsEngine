package me.anno.tests

import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader

fun main() {
    val text = "\"m2x2[][]:v\":[1,[1,[[0,1],[2,3]]]]}"
    TextReader(text, InvalidRef).readProperty(Saveable())
}