package me.anno.utils.structures.arrays

import me.anno.fonts.Codepoints.codepoints
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Strings.joinChars
import kotlin.math.max
import kotlin.math.min

class LineSequence : IntSequence {

    override val length get() = indexTable.last() - 1

    private var lines = ArrayList<IntArrayList>()
    private val indexTable = IntArrayList(16)

    var maxLineLength = 0
        private set

    init {
        lines.add(IntArrayList(16))
        indexTable.add(0)
        indexTable.add(1)
    }

    override fun get(index: Int): Int {
        val lineIndex = getLineIndexAt(index)
        val line = lines[lineIndex]
        val indexInLine = index - indexTable[lineIndex]
        assertTrue(indexInLine >= 0) {
            "$indexInLine by $index -> $lineIndex | ${
                indexTable.toList().joinToString()
            }"
        }
        return if (indexInLine < line.size) line[indexInLine] else '\n'.code
    }

    override fun getOrNull(index: Int): Int? {
        val lineIndex = getLineIndexAt(index)
        val line = lines.getOrNull(lineIndex) ?: return null
        val indexInLine = index - indexTable[lineIndex]
        return if (indexInLine < line.size) line.getOrNull(indexInLine) else '\n'.code
    }

    fun equals(target: CharSequence, startIndex: Int): Boolean {
        return equals(target.codepoints(), startIndex)
    }

    fun equals(target: IntArray, startIndex: Int): Boolean {
        val lineIndex = getLineIndexAt(startIndex)
        val line = lines.getOrNull(lineIndex) ?: return false
        val indexInLine = startIndex - indexTable[lineIndex]
        return getLineLength(lineIndex) >= target.size + indexInLine &&
                target.indices.all { ti -> target[ti] == line[indexInLine + ti] }
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
        return if (indexInLine < line.size) line[indexInLine] else '\n'.code
    }

    operator fun set(index: Int, newValue: Int): Int {
        val lineIndex = getLineIndexAt(index)
        val indexInLine = index - indexTable[lineIndex]
        return set(lineIndex, indexInLine, newValue)
    }

    operator fun set(lineIndex: Int, indexInLine: Int, newValue: Int): Int {
        val line = lines[lineIndex]
        val oldValue = if (indexInLine < line.size) line[indexInLine] else '\n'.code
        if (oldValue != newValue) {
            if (oldValue == '\n'.code || newValue == '\n'.code) {
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
        x0: Int, y0: Int, x1: Int, y1: Int,
        callback: LineSequenceCallback
    ) {
        for (lineIndex in max(y0, 0) until min(y1, lineCount)) {
            val line = lines[lineIndex]
            var index = indexTable[lineIndex] + max(x0, 0)
            for (indexInLine in max(x0, 0) until min(x1, line.size)) {
                callback.call(index, lineIndex, indexInLine, line[indexInLine])
                index++
            }
        }
    }

    fun insert(index: Int, char: Int) {
        synchronized(this) {
            val lineIndex = getLineIndexAt(index)
            val indexInLine = index - indexTable[lineIndex]
            insert(lineIndex, indexInLine, char)
        }
    }

    fun insert(lineIndex: Int, indexInLine: Int, char: Int) {
        synchronized(this) {
            val oldBuilder = lines[lineIndex]
            if (char == '\n'.code) {
                // split line
                val newBuilder = IntArrayList(16)
                newBuilder.add(oldBuilder, indexInLine)
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

    fun insert(lineIndex: Int, indexInLine: Int, sequence: IntArray) {
        synchronized(this) {
            for (index in sequence.indices.reversed()) {
                insert(lineIndex, indexInLine, sequence[index])
            }
        }
    }

    fun remove(index: Int): Int {
        return synchronized(this) {
            val lineIndex = getLineIndexAt(index)
            val indexInLine = index - indexTable[lineIndex]
            remove(lineIndex, indexInLine)
        }
    }

    fun remove(lineIndex: Int, indexInLine: Int): Int {
        return synchronized(this) {
            val oldBuilder = lines[lineIndex]
            if (indexInLine >= oldBuilder.size) {
                // merge lines
                val oldBuilder2 = lines[lineIndex + 1]
                oldBuilder.add(oldBuilder2)
                lines.removeAt(lineIndex + 1)
                maxLineLength = max(maxLineLength, oldBuilder.size)
                rebuildIndexTable()
                '\n'.code
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
                oldValue
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
            if (char == '\n'.code) {
                // split line
                lineIndex++
                if (lines.size > lineIndex) {
                    oldBuilder = lines[lineIndex]
                    oldBuilder.clear()
                } else {
                    oldBuilder = IntArrayList(16)
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
        insert(0, 0, text.codepoints())
    }

    override fun toString(): String {
        // could be allocation-optimized
        return lines.joinToString("\n") { it.joinChars() }
    }

    fun clear() {
        setText(null)
    }
}