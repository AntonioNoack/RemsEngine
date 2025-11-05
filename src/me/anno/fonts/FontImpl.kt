package me.anno.fonts

import me.anno.fonts.CharacterOffsetCache.Companion.getOffsetCache
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.IEmojiCache.Companion.emojiPadding
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.SizeLayoutHelper
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.image.raw.IntImage
import me.anno.utils.Color.undoPremultiply
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Strings.incrementTab
import me.anno.utils.types.Strings.isBlank
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Splits long text into multiple lines if required, and uses fallback fonts where necessary.
 * */
abstract class FontImpl<FallbackFonts> {

    companion object {

        val simpleChars0 = 33
        val simpleCharsLen = 126 + 1 - 33
        val simpleChars = IntArray(simpleCharsLen) { it + simpleChars0 }

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

    abstract fun getTextLength(font: Font, codepoint: Int): Int
    abstract fun getTextLength(font: Font, codepointA: Int, codepointB: Int): Int

    /**
     * Like gfx.drawString.
     * */
    abstract fun drawGlyph(
        image: IntImage, x0: Int, x1: Int, y0: Int, y1: Int, strictBounds: Boolean,
        font: Font, fallbackFonts: FallbackFonts, fontIndex: Int,
        codepoint: Int, textColor: Int, backgroundColor: Int, portableImages: Boolean,
    )

    /**
     * like gfx.drawString, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawEmoji(gfx: IntImage, font: Font, codepoint: Int, dx: Int, dy: Int) {
        val fontSize = font.sizeInt
        val emojiId = Codepoints.getEmojiId(codepoint)
        val emojiImage = IEmojiCache.emojiCache.getEmojiImage(emojiId, fontSize)
            .waitFor() ?: return
        // todo check these magic offsets for more fonts than just Verdana 20px
        val extraY = min(max(0, font.lineHeightI - fontSize), fontSize / 3)
        val yi = dy + extraY
        emojiImage.forEachPixel { pxi, pyi ->
            val color = emojiImage.getRGB(pxi, pyi).undoPremultiply()
            gfx.setRGB(dx + pxi - 2, yi + pyi - 2, color)
        }
    }

    fun generateTexture(
        font: Font, text: CharSequence,
        widthLimit: Int, heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int = -1, // white with full alpha
        backgroundColor: Int = 255 shl 24 // white with no alpha
    ) {
        if (text.isEmpty())
            return callback.ok(TextureLib.blackTexture)

        val fontSize = font.size
        val layout = GlyphLayout(
            font, text,
            widthLimitToRelative(widthLimit, fontSize),
            heightLimitToMaxNumLines(heightLimit, fontSize)
        )

        val width = min(layout.width, widthLimit)
        val height = min(layout.height, heightLimit)

        if (layout.isEmpty() || width < 1 || height < 1) {
            return callback.ok(FakeWhiteTexture(width, height, 1))
        }

        val texture = Texture2D("awt-font-v3", width, height, 1)

        val image = IntImage(texture.width, texture.height, true)
        if (backgroundColor.and(0xffffff) != 0) {
            // fill background with that color
            image.data.fill(backgroundColor)
        }

        val dy0 = layout.actualFontSize.toIntOr()
        val fallbackFonts = getFallbackFonts(font)
        for (glyphIndex in layout.indices) {
            val codepoint = layout.getCodepoint(glyphIndex)
            if (!Codepoints.isEmoji(codepoint)) {
                val fontIndex = layout.getFontIndex(glyphIndex)
                val x0 = layout.getX0(glyphIndex)
                val x1 = layout.getX1(glyphIndex)
                val dy = layout.getY(glyphIndex, font)
                drawGlyph(
                    image, x0, x1, dy, dy + dy0, false,
                    font, fallbackFonts, fontIndex,
                    codepoint, textColor, backgroundColor, portableImages
                )
            } // else will be drawn later
        }

        image.fillAlpha(0)
        for (glyphIndex in layout.indices) {
            val codepoint = layout.getCodepoint(glyphIndex)
            if (Codepoints.isEmoji(codepoint)) {
                val dx = layout.getX0(glyphIndex)
                val dy = layout.getY(glyphIndex, font)
                drawEmoji(image, font, codepoint, dx, dy)
            }
        }

        val hasPriority = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        if (hasPriority) {
            texture.create(image, false, callback)
        } else {
            addGPUTask("awt-font-v6", width, height, false) {
                texture.create(image, false, callback)
            }
        }
    }

    fun generateASCIITexture(
        font: Font, portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int = -1, // white with full alpha
        backgroundColor: Int = 255 shl 24 // white with no alpha
    ) {
        val widthLimit = GFX.maxTextureSize
        val heightLimit = GFX.maxTextureSize

        val alignment = getOffsetCache(font)
        val size = alignment.getOffset('w'.code, 'w'.code)
        val width = min(widthLimit, size + 1)
        val height = min(heightLimit, ceil(getLineHeight(font)).toIntOr())

        val texture = Texture2DArray("awtAtlas", width, height, simpleCharsLen)
        val image = IntImage(texture.width, texture.height * texture.layers, true)
        if (backgroundColor != 0) {
            // fill background with that color
            image.data.fill(backgroundColor)
        }
        var y = 0 // todo add this in AWTFont: fontMetrics.ascent.toFloat()
        val dy = texture.height
        val fallbackFonts = getFallbackFonts(font)
        val x1 = texture.width // correct?
        for (yi in 0 until simpleCharsLen) {
            val codepoint = simpleChars0 + yi
            drawGlyph(
                image, 0, x1, y, y + dy, true,
                font, fallbackFonts, getSupportLevel(fallbackFonts, codepoint, 0),
                codepoint, textColor, backgroundColor, portableImages
            )
            y += dy
        }

        image.fillAlpha(0)
        // there are no emojis in this range -> they can be skipped

        if (GFX.isGFXThread()) {
            texture.create(image, sync = true)
            callback.ok(texture)
        } else {
            addGPUTask("awtAtlas", width, height, false) {
                texture.create(image, sync = true)
                callback.ok(texture)
            }
        }
    }

    /**
     * distance from the top of generated textures to the lowest point of characters like A;
     * ~ 0.8 * fontSize, = ascent
     * */
    abstract fun getBaselineY(font: Font): Float

    /**
     * distance from the top of generated textures to the bottom;
     * ~ [1.0,1.5] * fontSize, = ascent + descent
     * */
    abstract fun getLineHeight(font: Font): Float

    abstract fun getFallbackFonts(font: Font): FallbackFonts
    abstract fun getSupportLevel(fonts: FallbackFonts, codepoint: Int, lastSupportLevel: Int): Int

    fun calculateSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        val helper = SizeLayoutHelper()
        fillGlyphLayout(
            font, text, helper,
            widthLimitToRelative(widthLimit, font.size),
            heightLimitToMaxNumLines(heightLimit, font.size)
        )
        return getSize(helper.width, helper.height)
    }

    fun fillGlyphLayout(
        font: Font, text: CharSequence,
        result: IGlyphLayout,
        relativeWidthLimit: Float,
        maxNumLines: Int,
    ) {

        val offsetCache = getOffsetCache(font)
        val fonts = getFallbackFonts(font)
        val codepoints = text.codepoints()

        val widthLimit = relativeWidthLimit * font.size
        val hasAutomaticLineBreak = widthLimit > 0f

        val spaceWidth = offsetCache.spaceWidth
        val extraSpacing = (font.relativeCharSpacing * spaceWidth).toIntOr()
        val tabSize = ((spaceWidth + extraSpacing) * font.relativeTabSize).toIntOr()
        val charSpacing = (font.size * font.relativeCharSpacing).toIntOr()

        var currentX = 0

        var startOfLine = result.size

        var wordEndI = 0

        fun finishLine() {
            val lineWidth = max(0, currentX - charSpacing)
            result.width = max(result.width, lineWidth)
            result.finishLine(startOfLine, result.size, lineWidth)
            @Suppress("AssignedValueIsNeverRead") // Intellij is broken
            startOfLine = result.size
        }

        fun nextLine() {
            finishLine()
            result.numLines++
            currentX = 0
        }

        fun pushWord(wordStartI: Int, wordEndI: Int, fontIndex: Int) {
            for (index in wordStartI until wordEndI) {
                val currCodepoint = codepoints[index]
                val nextCodepoint = if (index + 1 < wordEndI) codepoints[index + 1] else ' '.code
                val deltaX = offsetCache.getOffset(currCodepoint, nextCodepoint)
                val nextX = currentX + deltaX
                if (!currCodepoint.isBlank()) {
                    result.add(currCodepoint, currentX, nextX, result.numLines, fontIndex)
                }
                currentX = nextX + charSpacing
            }
        }

        fun skipSpace() {
            wordEndI++
        }

        fun nextLineIfAfterSpace() {
            if (hasAutomaticLineBreak && currentX > widthLimit) {
                nextLine()
            }
        }

        val emojiWidth = floor(font.size * (1f + emojiPadding))

        while (wordEndI < codepoints.size && result.numLines < maxNumLines) {
            when (val codepoint = codepoints[wordEndI]) {
                ' '.code -> {
                    currentX += spaceWidth + extraSpacing
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
                    val wordStartI = wordEndI++
                    var wordX = currentX
                    val fontIndex = getSupportLevel(fonts, codepoint, 0)
                    assertTrue(fontIndex >= 0)

                    var bestSplittingOrder = -1
                    var bestSplitIndex = wordStartI + 1

                    var currCodepoint = codepoint
                    var fitsOnThisLine = true
                    var fitsOnNextLine = true

                    while (true) {
                        var nextCodepoint = if (wordEndI < codepoints.size) codepoints[wordEndI] else ' '.code
                        val wordEndsHere = getSupportLevelEx(fonts, nextCodepoint, fontIndex) != fontIndex
                        if (wordEndsHere) nextCodepoint = ' '.code

                        wordEndI++

                        wordX += extraSpacing + offsetCache.getOffset(currCodepoint, nextCodepoint)
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

                        if (wordEndsHere) {
                            wordEndI--
                            break
                        }
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

        if ((currentX > 0f || result.numLines == 0) && result.numLines < maxNumLines) {
            nextLine()
        } else {
            finishLine()
        }

        // adding padding
        result.move(1, 2)
        result.width += 2
        result.height = result.numLines * font.lineHeightI + 1
    }

    private fun getSupportLevelEx(fonts: FallbackFonts, codepoint: Int, lastSupportLevel: Int): Int {
        if (Codepoints.isEmoji(codepoint) || (codepoint < 0xffff && codepoint.toChar() in " \t\r\n")) {
            return -1
        }
        return getSupportLevel(fonts, codepoint, lastSupportLevel)
    }
}