package me.anno.tests

import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.distance

fun main() {
    println("abc".distance("abcdef"))
    println("abcdef".distance("abc"))
    println("polyGeneLubricants".camelCaseToTitle())
}
