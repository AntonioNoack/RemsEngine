package me.anno.utils.test

import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap

fun main() {

    val map = StringMap()
    map["debug.ui.enableVsync"] = true
    println(map["debug.ui.enableVsync", true]) // true, correct
    map["debug.ui.enableVsync"] = false
    println(map["debug.ui.enableVsync", true]) // false, correct

    val asText = map.toString()
    val asMap = TextReader.read(asText)[0] as StringMap
    println(asMap["debug.ui.enableVsync"]) // null, incorrect

}