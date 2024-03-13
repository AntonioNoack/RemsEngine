package me.anno.tests.structures

import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader

fun main() {
    val text = "\"mat2d[][]:v\":[1,[1,[[0,1],[2,3]]]]}"
    JsonStringReader(text, InvalidRef).readProperty(Saveable())
}