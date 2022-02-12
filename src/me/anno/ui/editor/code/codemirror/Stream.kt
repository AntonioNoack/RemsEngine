package me.anno.ui.editor.code.codemirror

import me.anno.utils.structures.arrays.IntSequence

class Stream(val stream: IntSequence) {

    var index = 0
    var startIndex = 0

    fun eatSpace(): Boolean {
        return index < stream.length && when (stream[index].toChar()) {
            ' ', '\t', '\r', '\n' -> {
                index++
                true
            }
            else -> false
        }
    }

    fun next(): Char = if (index < stream.length) stream[index++].toChar() else 0.toChar()
    fun current() = stream.toDirtyCharSequence(startIndex, index)

    fun eat(char: Char): Boolean {
        return index < stream.length && if (stream[index].toChar() == char) {
            index++
            true
        } else false
    }

    fun eatWhile(regex: Regex) {
        while (index < stream.length && regex.matches(peek())) {
            index++
        }
    }

    fun peek() = stream.toDirtyCharSequence(index, stream.length)

}