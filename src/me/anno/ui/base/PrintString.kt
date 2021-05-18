package me.anno.ui.base

class PrintString(val value: String) {
    val asChars = value.toCharArray()
    val asCodePoints = value.codePoints()
    val isAscii = asChars.all { it.code < 128 }
}