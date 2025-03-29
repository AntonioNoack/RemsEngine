package me.anno.ui.editor.code.tokenizer

import me.anno.utils.structures.arrays.DirtyCharSequence
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
    fun current(): DirtyCharSequence = stream.toDirtyCharSequence(startIndex, index)

    fun startToken() {
        startIndex = index
    }

    fun isFinished(): Boolean {
        return startIndex == index
    }

    fun eat(char: Char): Boolean {
        return index < stream.length && if (stream[index].toChar() == char) {
            index++
            true
        } else false
    }

    fun eatWhile(test: (Char) -> Boolean) {
        while (index < stream.length && test(stream[index].toChar())) {
            index++
        }
    }

    fun peek() = stream.toDirtyCharSequence(index, stream.length)
}