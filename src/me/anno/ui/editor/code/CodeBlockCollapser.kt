package me.anno.ui.editor.code

import me.anno.utils.structures.arrays.LineSequence
import me.anno.utils.structures.arrays.LineSequenceCallback
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.sortedAdd

// todo these are WIP
class CodeBlockCollapser {

    private val blocks = ArrayList<CodeBlock>()

    fun open(mappedLine: Int) {
        val index = blocks.indexOfFirst { it.start == mappedLine }
        if (index >= 0) blocks.removeAt(index)
    }

    fun isClosed(mappedLine: Int): Boolean {
        return blocks.any2 { it.start == mappedLine }
    }

    fun close(block: CodeBlock) {
        blocks.sortedAdd(block, true)
    }

    fun mapLine(y: Int): Int {
        return y + blocks.sumOf {
            if (it.start < y) it.count
            else 0
        }
    }

    fun forEachChar(x0: Int, y0: Int, x1: Int, y1: Int, content: LineSequence, callback: LineSequenceCallback) {
        for (y in y0 until y1) {
            val mappedY = mapLine(y)
            content.forEachChar(x0, mappedY, x1, mappedY + 1) { charIndex, _, indexInLine, char ->
                callback.call(charIndex, y, indexInLine, char)
            }
        }
    }

    fun countLines(content: LineSequence): Int {
        return content.lineCount - blocks.sumOf { it.count }
    }
}