package me.anno.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.IEmojiCache.Companion.emojiPadding
import me.anno.gpu.GFX
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Arrays.indexOf
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.incrementTab
import me.anno.utils.types.Strings.isBlank
import me.anno.utils.types.Strings.joinChars
import kotlin.math.floor
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

        fun widthLimitToRelative(widthLimit: Int, fontSize: Float): Float {
            return if (widthLimit in 1 until GFX.maxTextureSize) {
                widthLimit / fontSize
            } else 0f
        }

        fun heightLimitToMaxNumLines(heightLimit: Int, fontSize: Float): Int {
            if (heightLimit <= 0) return Int.MAX_VALUE
            return (heightLimit / (fontSize + FontManager.spaceBetweenLines(fontSize))).roundToIntOr()
        }
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
        codepoints: IntArray,
        startIndex: Int,
        endIndex: Int,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        relativeWidthLimit: Float,
        result: PartResult,
    ) {

        val widthLimit = relativeWidthLimit * fontSize
        val hasAutomaticLineBreak = widthLimit > 0f
        val parts = result.parts
        val tabSize = getExampleAdvance() * relativeTabSize
        val charSpacing = fontSize * relativeCharSpacing
        var totalWidth = 0f
        var currentX = 0f
        var totalHeight = result.height
        val fontHeight = getActualFontHeight()
        var startOfLine = parts.size

        var index0 = startIndex
        var index1 = index0

        var lastSupportLevel = 0
        var numLines = result.numLines

        lateinit var nextLine: () -> Unit

        fun nextLineImpl() {
            val lineWidth = max(0f, currentX - charSpacing)
            totalWidth = max(totalWidth, lineWidth)
            for (i in startOfLine until parts.size) {
                parts[i].lineWidth = lineWidth
            }
            @Suppress("AssignedValueIsNeverRead") // Intellij is broken
            startOfLine = parts.size
            totalHeight += fontHeight
            numLines++
            currentX = 0f
        }

        fun display() {
            while (index0 < index1) {
                val advance = if (Codepoints.isEmoji(codepoints[index0])) {
                    floor(fontSize * (1f + emojiPadding))
                } else {
                    val font = fonts[lastSupportLevel]
                    val filtered = codepoints.joinChars(index0, index1)
                    getAdvance(filtered, font)
                }

                // if multiple chars and advance > lineWidth, then break line
                val numChars = index1 - index0
                val nextX = currentX + advance + numChars * charSpacing
                if (hasAutomaticLineBreak && numChars == 1 && currentX > 0f && nextX > widthLimit) {
                    nextLineImpl()
                    // just go to the next line, no splitting necessary
                } else if (hasAutomaticLineBreak && numChars > 1 && currentX > 0f && nextX > widthLimit) {
                    val index1Backup = index1
                    val splitIndex = findSplitIndex(codepoints, index0, index1, charSpacing, widthLimit, currentX)
                    index1 = splitIndex
                    if (index1 > index0 && codepoints[index1 - 1] == ' '.code && codepoints[index1 - 2] != ' '.code) index1-- // cut off last space

                    nextLine()

                    index0 = splitIndex
                    if (index1 == splitIndex && codepoints[index0] == ' '.code) index0++ // cut off first space
                    index1 = index1Backup
                } else {
                    if (nextX > currentX) {
                        val font = fonts[lastSupportLevel]
                        parts += StringPart.fromChars(
                            currentX, totalHeight, font, 0f,
                            codepoints, index0, index1, numLines
                        )
                    } // else just spaces, e.g. spaces with heart
                    currentX = nextX
                    totalWidth = max(totalWidth, currentX)
                    index0 = index1
                    break
                }
            }
        }

        nextLine = {
            display()
            nextLineImpl()
        }

        while (index1 < endIndex) {
            val codepoint = codepoints[index1]
            if (codepoint == '\t'.code) {

                display()

                index0++ // skip \t too
                currentX = incrementTab(currentX, tabSize, relativeTabSize)
                index1++

            } else if (Codepoints.isEmoji(codepoint)) {

                display() // show everything before the emoji
                index1++ // advance cursor to emoji

                display() // display emoji

            } else {
                val supportLevel = getSupportLevel(fonts, codepoint, lastSupportLevel)
                if (supportLevel != lastSupportLevel) {
                    display()
                    lastSupportLevel = supportLevel
                }

                index1++
            }
        }
        nextLine()

        result.width = totalWidth
        result.height = totalHeight
        result.numLines = numLines
    }

    fun splitParts(
        text: CharSequence,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        relativeWidthLimit: Float,
        maxNumLines: Int
    ): PartResult {

        val fonts = joinFonts(fontSize)
        val codepoints = text.codepoints()
        val actualFontSize = getActualFontHeight()

        val result = PartResult(ArrayList(), actualFontSize, 0f, 0f, 0)

        var i0 = 0
        while (i0 < codepoints.size) {
            var i1 = codepoints.indexOf('\n'.code, i0)
            if (i1 == -1) i1 = codepoints.size

            if ((i0 until i1).all { codepoints[it].isBlank() }) {

                result.numLines++
                result.height += actualFontSize
                if (result.numLines >= maxNumLines) break

            } else {

                val endIndex = if (i1 > 0 && codepoints[i1 - 1] == '\r'.code) i1 - 1 else i1
                splitLine(
                    fonts, codepoints, i0, endIndex, fontSize,
                    relativeTabSize, relativeCharSpacing, relativeWidthLimit, result
                )

                if (result.numLines >= maxNumLines) {
                    result.parts.removeIf { it.lineIndex >= maxNumLines }
                    break
                }
            }

            i0 = i1 + 1 // skip \n
        }

        return result
    }

    fun joinFonts(fontSize: Float): List<FontImpl> {
        val fallback = getFallbackFonts(fontSize)
        if (fallback.isEmpty()) return listOf(getSelfFont())

        val fonts = ArrayList<FontImpl>(fallback.size + 1)
        fonts.add(getSelfFont())
        fonts.addAll(fallback)
        return fonts
    }
}