package me.anno.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import kotlin.math.max
import kotlin.math.min

/**
 * Splits long text into multiple lines if required, and uses fallback fonts where necessary.
 * */
abstract class LineSplitter<FontImpl : TextGenerator> {

    companion object {
        private val splittingOrder: List<Collection<Int>> = listOf(
            listOf(' '.code),
            listOf('-'.code),
            "/\\:-*?=&|!#".map { it.code },
            listOf(','.code, '.'.code)
        )
    }

    abstract fun getAdvance(text: CharSequence, font: FontImpl): Float
    abstract fun getExampleAdvance(): Float

    abstract fun getSelfFont(): FontImpl
    abstract fun getFallbackFonts(size: Float): List<FontImpl>
    abstract fun getSupportLevel(fonts: List<FontImpl>, char: Int, lastSupportLevel: Int): Int

    fun getActualFontHeight(): Float = getSelfFont().getLineHeight()

    private fun findSplitIndex(
        chars: IntArray, index0: Int, index1: Int,
        charSpacing: Float, lineBreakWidth: Float, currentX: Float
    ): Int {
        // find the best index, where nextX <= lineBreakWidth
        val firstSplitIndex = index0 + 1
        val lastSplitIndex = index1 - 1
        return if (firstSplitIndex == lastSplitIndex) firstSplitIndex else {

            // calculation is expensive
            val font = getSelfFont()
            val listOfWidths = LazyList(lastSplitIndex - firstSplitIndex) {
                val splitIndex = firstSplitIndex + it
                val substring2 = chars.joinChars(index0, splitIndex)
                val advance2 = getAdvance(substring2, font)
                advance2 + (splitIndex - index0) * charSpacing // width
            }

            val delta = lineBreakWidth - currentX
            var lastValidSplitIndex = listOfWidths.binarySearch { it.compareTo(delta) }
            if (lastValidSplitIndex < 0) lastValidSplitIndex = -1 - lastValidSplitIndex
            lastValidSplitIndex = max(0, lastValidSplitIndex - 1)

            val charsOfInterest = firstSplitIndex + lastValidSplitIndex downTo firstSplitIndex
            var foundSolution = false
            search@ for (splittingChars in splittingOrder) {
                for (index in charsOfInterest) {
                    if (chars[index] in splittingChars) {
                        // found the best splitting char <3
                        lastValidSplitIndex = min(index + 1, lastValidSplitIndex)
                        foundSolution = true
                        break@search
                    }
                }
            }

            if (!foundSolution) {// prefer to split upper case characters
                search@ for (index in charsOfInterest) {
                    val char = chars[index].toChar()
                    if (char.isUpperCase()) {
                        lastValidSplitIndex = min(index, lastValidSplitIndex)
                        break@search
                    }
                }
            }

            firstSplitIndex + lastValidSplitIndex
        }
    }

    private fun splitLine(
        fonts: List<FontImpl>,
        line: String,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        lineBreakWidth: Float
    ): PartResult {

        val hasAutomaticLineBreak = lineBreakWidth > 0f
        val parts = ArrayList<StringPart>()
        val tabSize = getExampleAdvance() * relativeTabSize
        val charSpacing = fontSize * relativeCharSpacing
        var totalWidth = 0f
        var currentX = 0f
        var totalHeight = 0f
        val fontHeight = getActualFontHeight()
        var startResultIndex = 0

        val chars = line.codepoints()
        var index0 = 0
        var index1 = 0
        var lastSupportLevel = 0

        lateinit var nextLine: () -> Unit

        fun display() {
            while (true) {
                if (index1 > index0) {
                    val font = fonts[lastSupportLevel]
                    val filtered = chars.joinChars(index0, index1) {
                        it !in 0xfe00..0xfe0f // Emoji variations; having no width, even if Java thinks so
                    }
                    val advance = if (filtered.isNotEmpty()) {
                        getAdvance(filtered, font)
                    } else 0f
                    // if multiple chars and advance > lineWidth, then break line
                    val nextX = currentX + advance + (index1 - index0) * charSpacing
                    if (hasAutomaticLineBreak && index0 + 1 < index1 && currentX == 0f && nextX > lineBreakWidth) {
                        val tmp1 = index1
                        val splitIndex = findSplitIndex(chars, index0, index1, charSpacing, lineBreakWidth, currentX)
                        /*LOGGER.info("split [$line $fontSize $lineBreakWidth] $substring into " +
                                chars.subList(index0,splitIndex).joinChars() +
                                " + " +
                                chars.subList(splitIndex,index1).joinChars()
                        )*/
                        index1 = splitIndex
                        if (index1 > index0 && chars[index1 - 1] == ' '.code && chars[index1 - 2] != ' '.code) index1-- // cut off last space
                        nextLine()
                        index0 = splitIndex
                        if (index1 == splitIndex && chars[index0] == ' '.code) index0++ // cut off first space
                        index1 = tmp1
                    } else {
                        parts += StringPart(currentX, totalHeight, font, chars.joinChars(index0, index1), 0f)
                        currentX = nextX
                        totalWidth = max(totalWidth, currentX)
                        index0 = index1
                        break
                    }
                } else break
            }
        }

        nextLine = {
            display()
            val lineWidth = max(0f, currentX - charSpacing)
            totalWidth = max(totalWidth, lineWidth)
            for (i in startResultIndex until parts.size) {
                parts[i].lineWidth = lineWidth
            }
            @Suppress("AssignedValueIsNeverRead") // Intellij is broken
            startResultIndex = parts.size
            totalHeight += fontHeight
            currentX = 0f
        }

        while (index1 < chars.size) {
            when (val char = chars[index1]) {
                '\t'.code -> {
                    display()
                    index0++ // skip \t too
                    currentX = Strings.incrementTab(currentX, tabSize, relativeTabSize)
                }
                else -> {
                    val supportLevel = getSupportLevel(fonts, char, lastSupportLevel)
                    if (supportLevel != lastSupportLevel) {
                        display()
                        lastSupportLevel = supportLevel
                    }
                }
            }
            index1++
        }
        nextLine()

        val lineCount = max((totalHeight / fontHeight).roundToIntOr(), 1)
        return PartResult(parts, totalWidth, totalHeight, lineCount)
    }

    // used in Rem's Studio
    fun splitParts(
        text: CharSequence,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        lineBreakWidth: Float,
        textBreakHeight: Float
    ): PartResult {

        val fallback = getFallbackFonts(fontSize)
        val fonts = ArrayList<FontImpl>(fallback.size + 1)

        fonts.add(getSelfFont())
        fonts.addAll(fallback)

        val lineCountLimit = if (textBreakHeight < 0f) Int.MAX_VALUE
        else (textBreakHeight / (fontSize + FontManager.spaceBetweenLines(fontSize))).roundToIntOr()

        var lines = text.split('\n')
        if (lines.size > lineCountLimit) lines = lines.subList(0, lineCountLimit)

        val splitLines = lines.map {
            if (it.isBlank2()) null
            else splitLine(fonts, it, fontSize, relativeTabSize, relativeCharSpacing, lineBreakWidth)
        }

        val width = splitLines.maxByOrNull { it?.width ?: 0f }?.width ?: 0f
        val actualFontSize = getActualFontHeight()

        var lineCount = 0
        val parts = ArrayList<StringPart>(splitLines.sumOf { it?.parts?.size ?: 0 })
        for (i in splitLines.indices) {
            val partResult = splitLines[i]
            lineCount += if (partResult != null) {
                if (lineCount == 0) {
                    parts.addAll(partResult.parts)
                } else {
                    val offsetY = actualFontSize * lineCount
                    parts.addAll(partResult.parts.map { it.plus(offsetY) })
                }
                partResult.lineCount
            } else 1
        }
        val height = lineCount * actualFontSize
        return PartResult(parts, width, height, lineCount)
    }
}