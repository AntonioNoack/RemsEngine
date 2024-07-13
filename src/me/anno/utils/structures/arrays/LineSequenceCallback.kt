package me.anno.utils.structures.arrays

fun interface LineSequenceCallback {
    fun call(charIndex: Int, lineIndex: Int, indexInLine: Int, char: Int)
}