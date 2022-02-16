package me.anno.utils.structures.arrays

import me.anno.utils.types.Strings.joinChars
import java.util.stream.IntStream
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

class LineSequence : IntSequence {

    // todo line sequence with automatic line breaks on words

    override val length get() = indexTable.last() - 1

    private var lines = ArrayList<ExpandingIntArray>()
    private val indexTable = ExpandingIntArray(16)

    var maxLineLength = 0
        private set

    init {
        lines.add(ExpandingIntArray(16))
        indexTable.add(0)
        indexTable.add(1)
    }

    fun removeLine(lineIndex: Int) {
        lines.removeAt(lineIndex)
        rebuildIndexTable()
        maxLineLength = lines.maxOf { it.size }
    }

    override fun get(index: Int): Int {
        val lineIndex = getLineIndexAt(index)
        val line = lines[lineIndex]
        val indexInLine = index - indexTable[lineIndex]
        return if (indexInLine < line.size) line[indexInLine] else newLine
    }

    val lineCount get() = lines.size

    fun getLineStart(lineIndex: Int) = indexTable[lineIndex]
    fun getLineEnd(lineIndex: Int) = indexTable[lineIndex + 1]
    fun getLineLength(lineIndex: Int) = lines[lineIndex].size

    fun getLineIndexAt(index: Int): Int {
        var lineIndex = indexTable.binarySearch(index)
        if (lineIndex < 0) lineIndex = -2 - lineIndex
        return lineIndex
    }

    operator fun get(lineIndex: Int, indexInLine: Int): Int {
        val line = lines[lineIndex]
        return if (indexInLine < line.size) line[indexInLine] else newLine
    }

    operator fun set(index: Int, newValue: Int): Int {
        val lineIndex = getLineIndexAt(index)
        val indexInLine = index - indexTable[lineIndex]
        return set(lineIndex, indexInLine, newValue)
    }

    operator fun set(lineIndex: Int, indexInLine: Int, newValue: Int): Int {
        val line = lines[lineIndex]
        val oldValue = if (indexInLine < line.size) line[indexInLine] else newLine
        if (oldValue != newValue) {
            if (oldValue == newLine || newValue == newLine) {
                // if old value is \n, then change stuff
                // if new value is \n, then split line
                remove(lineIndex, indexInLine)
                insert(lineIndex, indexInLine, newValue)
            } else {
                line[indexInLine] = newValue
            }
        }
        return oldValue
    }

    fun getIndexAt(lineIndex: Int, indexInLine: Int): Int {
        return indexTable[lineIndex] + indexInLine
    }

    private fun rebuildIndexTable() {
        indexTable.clear()
        indexTable.add(0)
        var length = 0
        for (line in lines) {
            val lineLength = line.size
            length += lineLength + 1
            indexTable.add(length)
        }
    }

    fun forEachChar(
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        run: (index: Int, lineIndex: Int, indexInLine: Int, char: Int) -> Unit
    ) {
        for (lineIndex in max(y0, 0) until min(y1, lineCount)) {
            val line = lines[lineIndex]
            var index = indexTable[lineIndex] + max(x0, 0)
            for (indexInLine in max(x0, 0) until min(x1, line.size)) {
                run(index, lineIndex, indexInLine, line[indexInLine])
                index++
            }
        }
    }

    fun insert(index: Int, Int: Int) {
        synchronized(this) {
            val lineIndex = getLineIndexAt(index)
            val indexInLine = index - indexTable[lineIndex]
            insert(lineIndex, indexInLine, Int)
        }
    }

    fun insert(lineIndex: Int, indexInLine: Int, char: Int) {
        synchronized(this) {
            val oldBuilder = lines[lineIndex]
            if (char == newLine) {
                // split line
                val newBuilder = ExpandingIntArray(16)
                newBuilder.add(oldBuilder, indexInLine, oldBuilder.size)
                oldBuilder.removeBetween(indexInLine, oldBuilder.size)
                lines.add(lineIndex + 1, newBuilder)
                maxLineLength = lines.maxOf { it.size }
                rebuildIndexTable()
            } else {
                oldBuilder.add(indexInLine, char)
                maxLineLength = max(maxLineLength, oldBuilder.size)
                for (i in lineIndex + 1 until indexTable.size) {
                    indexTable.inc(i)
                }
            }
        }
    }

    fun insert(lineIndex: Int, indexInLine: Int, sequence: IntSequence) {
        synchronized(this) {
            for (index in sequence.indices.reversed()) {
                insert(lineIndex, indexInLine, sequence[index])
            }
        }
    }

    fun insert(lineIndex: Int, indexInLine: Int, sequence: IntStream) {
        synchronized(this) {
            val list = sequence.toList()
            for (index in list.indices.reversed()) {
                insert(lineIndex, indexInLine, list[index])
            }
        }
    }

    fun insert(lineIndex: Int, indexInLine: Int, sequence: List<Int>) {
        synchronized(this) {
            for (index in sequence.reversed().indices) {
                insert(lineIndex, indexInLine, sequence[index])
            }
        }
    }

    fun remove(index: Int): Int {
        synchronized(this) {
            val lineIndex = getLineIndexAt(index)
            val indexInLine = index - indexTable[lineIndex]
            return remove(lineIndex, indexInLine)
        }
    }

    fun remove(lineIndex: Int, indexInLine: Int): Int {
        synchronized(this) {
            val oldBuilder = lines[lineIndex]
            if (indexInLine >= oldBuilder.size) {
                // merge lines
                val oldBuilder2 = lines[lineIndex + 1]
                oldBuilder.add(oldBuilder2)
                lines.removeAt(lineIndex + 1)
                maxLineLength = max(maxLineLength, oldBuilder.size)
                rebuildIndexTable()
                return newLine
            } else {
                val oldValue = oldBuilder[indexInLine]
                // just remove a Int
                oldBuilder.removeAt(indexInLine)
                for (i in lineIndex + 1 until indexTable.size) {
                    indexTable.dec(i)
                }
                if (oldBuilder.size + 1 >= maxLineLength) {
                    maxLineLength = lines.maxOf { it.size }
                }
                // rebuildIndexTable()
                return oldValue
            }
        }
    }

    override fun subSequence(startIndex: Int, endIndex: Int): IntSequence {
        if (startIndex == 0 && endIndex == length) return this
        return IntSequenceView(this, startIndex, endIndex)
    }

    fun setText(text: IntSequence?) {
        var lineIndex = 0
        var oldBuilder = lines[0]
        oldBuilder.clear()
        maxLineLength = 0
        if (text != null) for (index in text.indices) {
            val char = text[index]
            if (char == newLine) {
                // split line
                lineIndex++
                if (lines.size > lineIndex) {
                    oldBuilder = lines[lineIndex]
                    oldBuilder.clear()
                } else {
                    oldBuilder = ExpandingIntArray(16)
                    lines.add(oldBuilder)
                }
            } else {
                oldBuilder.add(char)
                maxLineLength = max(maxLineLength, oldBuilder.size)
            }
        }
        while (lines.size > lineIndex + 1) lines.removeAt(lines.lastIndex)
        rebuildIndexTable()
    }


    fun setText(text: CharSequence) {
        clear()
        insert(0, 0, text.codePoints())
    }

    override fun toString(): String {
        return lines.joinToString("\n") { it.joinChars() }
    }

    fun clear() {
        setText(null)
    }

    companion object {

        private val newLine = '\n'.code

        @JvmStatic
        fun main(args: Array<String>) {
            val ls = LineSequence()
            ls.setText(
                "this is great\n" +
                        "isn't it?"
            )
            println(ls)
            ls[0, 0] = 'T'.code
            ls[1, 0] = 'I'.code
            ls.insert(1, "isn't it".length, '!'.code)
            println(ls)
            ls.remove(1, "isn't it!".length)
            println(ls)
        }
    }

}