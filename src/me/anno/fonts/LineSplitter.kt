package me.anno.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.IEmojiCache.Companion.emojiPadding
import me.anno.gpu.GFX
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.incrementTab
import me.anno.utils.types.Strings.isBlank
import kotlin.math.floor
import kotlin.math.max

/**
 * Splits long text into multiple lines if required, and uses fallback fonts where necessary.
 * */
abstract class LineSplitter<FontImpl : TextGenerator> {

    companion object {
        private val splittingOrder = listOf(
            intArrayOf(' '.code),
            intArrayOf('-'.code),
            "/\\:-*?=&|!#".codepoints(),
            intArrayOf(','.code, '.'.code)
        )

        fun getSplittingOrder(codepoint: Int): Int {
            var order = splittingOrder.size
            for (entry in splittingOrder) {
                if (codepoint in entry) {
                    return order
                }
                order--
            }
            return -1
        }

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
    abstract fun getSupportLevel(fonts: List<FontImpl>, codepoint: Int, lastSupportLevel: Int): Int

    fun getActualFontHeight(): Float = getSelfFont().getLineHeight()

    fun fillGlyphLayout(
        result: GlyphLayout,
        relativeWidthLimit: Float,
        maxNumLines: Int,
        offsetCache: CharacterOffsetCache
    ) {

        val fonts = getFontAndFallbacks(result.font.size)
        val codepoints = result.text.codepoints()

        val font = result.font
        val widthLimit = relativeWidthLimit * font.size
        val hasAutomaticLineBreak = widthLimit > 0f
        val tabSize = getExampleAdvance() * font.relativeTabSize
        val charSpacing = font.size * font.relativeCharSpacing

        var currentX = 0f

        val fontHeight = result.actualFontSize
        var startOfLine = result.size

        var wordEndI = 0

        fun nextLine() {
            val lineWidth = max(0f, currentX - charSpacing)
            result.width = max(result.width, lineWidth)
            for (i in startOfLine until result.size) {
                result.setLineWidth(i, lineWidth)
            }
            @Suppress("AssignedValueIsNeverRead") // Intellij is broken
            startOfLine = result.size
            result.height += fontHeight
            result.numLines++
            currentX = 0f
        }

        fun pushWord(wordStartI: Int, wordEndI: Int, fontIndex: Int) {
            for (index in wordStartI until wordEndI) {
                val currCodepoint = codepoints[index]
                val nextCodepoint = if (index + 1 < wordEndI) codepoints[index + 1] else ' '.code
                val nextX = currentX + offsetCache.getOffset(currCodepoint, nextCodepoint)
                if (!currCodepoint.isBlank()) {
                    result.add(
                        currCodepoint, currentX, nextX, 0f, result.height, result.numLines,
                        fontIndex
                    )
                }
                currentX = nextX
            }
            result.width = max(result.width, currentX)
        }

        fun skipSpace() {
            result.width = max(result.width, currentX)
            wordEndI++
        }

        fun nextLineIfAfterSpace() {
            if (hasAutomaticLineBreak && currentX > widthLimit) {
                nextLine()
            }
        }

        val emojiWidth = floor(font.size * (1f + emojiPadding))
        val spaceWidth = offsetCache.spaceWidth.toFloat()

        while (wordEndI < codepoints.size && result.numLines < maxNumLines) {
            when (val codepoint = codepoints[wordEndI]) {
                ' '.code -> {
                    currentX += (1f + font.relativeCharSpacing) * spaceWidth
                    skipSpace()
                    nextLineIfAfterSpace()
                }
                '\t'.code -> {
                    currentX = incrementTab(currentX, tabSize, font.relativeTabSize)
                    skipSpace()
                    nextLineIfAfterSpace()
                }
                '\r'.code, '\n'.code -> {
                    nextLine()
                    skipSpace()
                    if (codepoint == '\r'.code && // handle \r\n as a single line break
                        wordEndI < codepoints.size &&
                        codepoints[wordEndI] == '\n'.code
                    ) skipSpace()
                }
                else -> if (Codepoints.isEmoji(codepoint)) {
                    // handle emoji
                    if (hasAutomaticLineBreak && currentX > 0f && currentX + emojiWidth > widthLimit) {
                        nextLine()
                    }
                    pushWord(wordEndI, wordEndI + 1, -1) // display emoji
                    wordEndI++ // advance cursor to emoji
                } else {

                    // read a normal word
                    val wordStartI = wordEndI
                    var wordX = currentX
                    val fontIndex = getSupportLevel(fonts, codepoint, 0)
                    assertTrue(fontIndex >= 0)

                    var bestSplittingOrder = -1
                    var bestSplitIndex = wordStartI

                    var currCodepoint = codepoint
                    var fitsOnThisLine = true
                    var fitsOnNextLine = true
                    while (wordEndI < codepoints.size) {
                        val nextCodepoint = codepoints[wordEndI++]
                        if (getSupportLevel(fonts, nextCodepoint, fontIndex) != fontIndex) {
                            break // found end of word
                        }

                        val advance = offsetCache.getOffset(currCodepoint, nextCodepoint)
                        wordX += advance
                        if (hasAutomaticLineBreak && wordX > widthLimit) {
                            fitsOnThisLine = false
                        } else {
                            val splittingOrder = getSplittingOrder(currCodepoint)
                            if (splittingOrder >= bestSplittingOrder) {
                                bestSplittingOrder = splittingOrder
                                bestSplitIndex = wordEndI - 1 // after currCodepoint
                            }
                        }
                        if (hasAutomaticLineBreak && wordX - currentX > widthLimit) {
                            fitsOnNextLine = false
                            break
                        }

                        currCodepoint = nextCodepoint
                    }

                    when {
                        fitsOnThisLine -> {
                            pushWord(wordStartI, wordEndI, fontIndex)
                            // damn, easy
                        }
                        fitsOnNextLine -> {
                            nextLine()
                            if (result.numLines >= maxNumLines) return // done
                            pushWord(wordStartI, wordEndI, fontIndex)
                            // damn, easy, too
                        }
                        else -> {
                            wordEndI = bestSplitIndex
                            pushWord(wordStartI, bestSplitIndex, fontIndex)
                            nextLine()
                            // more of the word follows next iteration
                        }
                    }
                }
            }
        }

        if (result.numLines >= maxNumLines) return // done
        nextLine()
    }

    fun getFontAndFallbacks(fontSize: Float): List<FontImpl> {
        val fallback = getFallbackFonts(fontSize)
        if (fallback.isEmpty()) return listOf(getSelfFont())

        val fonts = ArrayList<FontImpl>(fallback.size + 1)
        fonts.add(getSelfFont())
        fonts.addAll(fallback)
        return fonts
    }
}