package me.anno.jvm.fonts

import me.anno.config.DefaultConfig
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.FontManager
import me.anno.fonts.FontManager.spaceBetweenLines
import me.anno.fonts.PartResult
import me.anno.fonts.StringPart
import me.anno.fonts.TextGenerator
import me.anno.fonts.TextGroup
import me.anno.fonts.mesh.CharacterOffsetCache
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts.simpleChars
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.TextureLib
import me.anno.jvm.fonts.DefaultRenderingHints.prepareGraphics
import me.anno.jvm.images.BIImage.createFromBufferedImage
import me.anno.jvm.images.BIImage.toImage
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.Callback
import me.anno.utils.structures.lists.ExpensiveList
import me.anno.utils.types.Strings.incrementTab
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import me.anno.utils.types.Strings.shorten
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AWTFont(
    val font: me.anno.fonts.Font, // used in Rem's Studio -> don't make it private
    val awtFont: Font
) : TextGenerator {

    private val fontMetrics = run {
        val unused = BufferedImage(1, 1, 1).graphics as Graphics2D
        unused.prepareGraphics(awtFont, false)
        unused.fontMetrics
    }

    private fun CharSequence.containsSpecialChar(): Boolean {
        val limit = 127.toChar()
        for (ci in indices) {
            val cp = get(ci)
            if (cp == '\n' || cp == '\t' || cp > limit) return true
        }
        return false
    }

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, text: CharSequence, group: TextGroup?, y: Int) =
        drawString(gfx, text, group, 0f, y.toFloat())

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, text: CharSequence, group: TextGroup?, x: Float, y: Float) {
        val group2 = group ?: createGroup(font, text)
        // some distances still are awkward, because it is using the closest position, not float
        // (useful for "I"s)
        // maybe we could implement detecting, which sections need int positions, and which don't...
        if (text.containsSpecialChar()) {
            for ((index, char) in text.codepoints().withIndex()) {
                gfx.drawString(
                    char.joinChars().toString(),
                    x + group2.offsets[index].toFloat(), y
                )
            }
        } else {
            for (index in text.indices) {
                val char = text[index]
                gfx.drawString(
                    asciiStrings[char.code],
                    x + group2.offsets[index].toFloat(), y
                )
            }
        }
    }

    override fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        if (text.isEmpty()) return GFXx2D.getSize(0, font.sizeInt)
        return if (text.containsSpecialChar() || (widthLimit in 0 until GFX.maxTextureSize)) {
            val parts = splitParts(
                text, font.size, 4f, 0f,
                widthLimit.toFloat(), heightLimit.toFloat()
            )
            val width = min(ceil(parts.width).toInt(), widthLimit)
            val height = min(ceil(parts.height).toInt(), heightLimit)
            return GFXx2D.getSize(width, height)
        } else {
            val baseWidth = getStringWidth(createGroup(font, text))
            val width = clamp(baseWidth.roundToInt() + 1, 0, GFX.maxTextureSize)
            val height = min(fontMetrics.height, GFX.maxTextureSize)
            GFXx2D.getSize(width, height)
        }
    }

    override fun generateTexture(
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int,
        backgroundColor: Int,
        extraPadding: Int
    ) {

        if (text.isEmpty()) {
            return callback.ok(TextureLib.blackTexture)
        }

        if (text.containsSpecialChar() || widthLimit < text.length * font.size * 2f) {
            return generateTextureV3(
                text, font.size, widthLimit, heightLimit, portableImages,
                textColor, backgroundColor, extraPadding, callback
            )
        }

        val group = createGroup(font, text)
        val width = min(widthLimit, getStringWidth(group).roundToInt() + 1 + 2 * extraPadding)

        val lineCount = 1
        val fontHeight = fontMetrics.height
        val height = min(heightLimit, fontHeight * lineCount + 2 * extraPadding)

        if (width < 1 || height < 1) {
            return callback.ok(TextureLib.blackTexture)
        }
        if (max(width, height) > GFX.maxTextureSize) {
            return callback.err(
                IllegalArgumentException(
                    "Texture for text is too large! $width x $height > ${GFX.maxTextureSize}, " +
                            "${text.length} chars, $lineCount lines, ${awtFont.name} ${font.size} px, ${
                                text.toString().shorten(200)
                            }"
                )
            )
        }

        if (text.isBlank2()) {
            // we need some kind of wrapper around texture2D
            // and return an empty/blank texture
            // that the correct size is returned is required by text input fields
            // (with whitespace at the start or end)
            return callback.ok(FakeWhiteTexture(width, height, 1))
        }

        val texture = Texture2D("awt-" + text.shorten(24), width, height, 1)
        val hasPriority = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        val image = createImage(
            width,
            height, portableImages, textColor,
            backgroundColor, extraPadding, text, group
        )
        if (hasPriority) {
            texture.createFromBufferedImage(image, sync = true, checkRedundancy = false)?.invoke()
            callback.ok(texture)
        } else {
            GFX.addGPUTask("awt-font-v5", width, height) {
                texture.createFromBufferedImage(image, sync = true, checkRedundancy = false)?.invoke()
                callback.ok(texture)
            }
        }
    }

    override fun generateASCIITexture(
        portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int,
        backgroundColor: Int,
        extraPadding: Int
    ) {

        val widthLimit = GFX.maxTextureSize
        val heightLimit = GFX.maxTextureSize

        val alignment = CharacterOffsetCache.getOffsetCache(font)
        val size = alignment.getOffset('w'.code, 'w'.code)
        val width = min(widthLimit, size.roundToInt() + 1 + 2 * extraPadding)
        val height = min(heightLimit, fontMetrics.height + 2 * extraPadding)

        val texture = Texture2DArray("awtAtlas", width, height, simpleChars.size)
        if (GFX.isGFXThread()) {
            createASCIITexture(texture, portableImages, textColor, backgroundColor, extraPadding)
            callback.ok(texture)
        } else {
            GFX.addGPUTask("awtAtlas", width, height) {
                createASCIITexture(texture, portableImages, textColor, backgroundColor, extraPadding)
                callback.ok(texture)
            }
        }
    }

    private fun createImage(
        width: Int, height: Int, portableImages: Boolean,
        textColor: Int, backgroundColor: Int, extraPadding: Int,
        text: CharSequence, group: TextGroup?,
    ): BufferedImage {
        val image = BufferedImage(width, height, 1)
        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)

        if (backgroundColor != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, width, height)
        }

        if (extraPadding != 0) {
            gfx.translate(extraPadding, extraPadding)
        }

        gfx.color = Color(textColor)

        val y = fontMetrics.ascent
        // println("generating texture for '$text', size $fontSize with ascent $ascent")
        drawString(gfx, text, group, y)
        gfx.dispose()
        return image
    }

    private val renderContext by lazy {
        FontRenderContext(null, true, true)
    }

    // used in Rem's Studio
    val exampleLayout by lazy {
        TextLayout("o", awtFont, renderContext)
    }

    private val actualFontSize by lazy {
        exampleLayout.ascent + exampleLayout.descent
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

        val fallback = getFallback(fontSize)
        val fonts = ArrayList<AWTFont>(fallback.size + 1)

        fonts += this
        fonts += fallback

        val lineCountLimit = if (textBreakHeight < 0f) Int.MAX_VALUE
        else (textBreakHeight / (fontSize + spaceBetweenLines(fontSize))).roundToInt()

        var lines = text.split('\n')
        if (lines.size > lineCountLimit) lines = lines.subList(0, lineCountLimit)

        val splitLines = lines.map {
            if (it.isBlank2()) null
            else splitLine(fonts, it, fontSize, relativeTabSize, relativeCharSpacing, lineBreakWidth)
        }

        val width = splitLines.maxByOrNull { it?.width ?: 0f }?.width ?: 0f

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

    private fun getSupportLevel(fonts: List<AWTFont>, char: Int, lastSupportLevel: Int): Int {
        for (index in fonts.indices) {
            val font = fonts[index]
            if (font.awtFont.canDisplay(char)) return index
        }
        return lastSupportLevel
    }

    private fun findSplitIndex(
        chars: IntArray, index0: Int, index1: Int,
        charSpacing: Float, lineBreakWidth: Float, currentX: Float
    ): Int {
        // find the best index, where nextX <= lineBreakWidth
        val firstSplitIndex = index0 + 1
        val lastSplitIndex = index1 - 1
        return if (firstSplitIndex == lastSplitIndex) firstSplitIndex else {

            // calculation is expensive
            val listOfWidths = ExpensiveList(lastSplitIndex - firstSplitIndex) {
                val splitIndex = firstSplitIndex + it
                val substring2 = chars.joinChars(index0, splitIndex)
                val advance2 = TextLayout(substring2.toString(), awtFont, renderContext).advance
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
        fonts: List<AWTFont>,
        line: String,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        lineBreakWidth: Float
    ): PartResult {

        val hasAutomaticLineBreak = lineBreakWidth > 0f
        val result = ArrayList<StringPart>()
        val tabSize = exampleLayout.advance * relativeTabSize
        val charSpacing = fontSize * relativeCharSpacing
        var widthF = 0f
        var currentX = 0f
        var currentY = 0f
        val fontHeight = actualFontSize
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
                    val advance = if (filtered.isNotEmpty())
                        TextLayout(filtered.toString(), font.awtFont, renderContext).advance else 0f
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
                        result += StringPart(currentX, currentY, font, chars.joinChars(index0, index1), 0f)
                        currentX = nextX
                        widthF = max(widthF, currentX)
                        index0 = index1
                        break
                    }
                } else break
            }
        }

        nextLine = {
            display()
            val lineWidth = max(0f, currentX - charSpacing)
            for (i in startResultIndex until result.size) {
                result[i].lineWidth = lineWidth
            }
            startResultIndex = result.size
            currentY += fontHeight
            currentX = 0f
        }

        while (index1 < chars.size) {
            when (val char = chars[index1]) {
                '\t'.code -> {
                    display()
                    index0++ // skip \t too
                    currentX = incrementTab(currentX, tabSize, relativeTabSize)
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

        val lineCount = max((currentY / actualFontSize).roundToInt(), 1)
        return PartResult(result, widthF, currentY, lineCount)
    }

    private fun generateTextureV3(
        text: CharSequence,
        fontSize: Float,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        textColor: Int,
        backgroundColor: Int,
        extraPadding: Int,
        callback: Callback<ITexture2D>
    ) {

        val parts = splitParts(text, fontSize, 4f, 0f, widthLimit.toFloat(), heightLimit.toFloat())
        val result = parts.parts

        val width = min(ceil(parts.width).toInt() + 2 + 2 * extraPadding, widthLimit)
        val height = min(ceil(parts.height).toInt() + 1 + 2 * extraPadding, heightLimit)

        if (result.isEmpty() || width < 1 || height < 1) {
            return callback.ok(FakeWhiteTexture(width, height, 1))
        }

        val texture = Texture2D("awt-font-v3", width, height, 1)
        val hasPriority = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        if (hasPriority) {
            createImage(texture, portableImages, textColor, backgroundColor, extraPadding, result)
            callback.ok(texture)
        } else {
            GFX.addGPUTask("awt-font-v6", width, height) {
                createImage(texture, portableImages, textColor, backgroundColor, extraPadding, result)
                callback.ok(texture)
            }
        }
    }

    private fun createImage(
        texture: Texture2D, portableImages: Boolean, textColor: Int, backgroundColor: Int,
        extraPadding: Int, result: List<StringPart>
    ) {
        val image = BufferedImage(texture.width, texture.height, 1)
        // for (i in width-10 until width) image.setRGB(i, 0, 0xff0000)

        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)
        gfx.background = Color(backgroundColor)
        if (backgroundColor.and(0xffffff) != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, image.width, image.height)
        }
        gfx.translate(extraPadding, extraPadding)
        gfx.color = Color(textColor)

        val y = exampleLayout.ascent

        for (part in result) {
            gfx.font = (part.font as AWTFont).awtFont
            // s.font != this when the character is unsupported, e.g., for emojis
            (part.font as AWTFont).drawString(gfx, part.text, null, part.xPos, part.yPos + y)
        }

        gfx.dispose()
        texture.createFromBufferedImage(image, sync = true, checkRedundancy = false)?.invoke()
    }

    private fun createASCIITexture(
        texture: Texture2DArray,
        portableImages: Boolean,
        textColor: Int,
        backgroundColor: Int,
        extraPadding: Int
    ) {
        val image = BufferedImage(texture.width, texture.height * texture.layers, 1)
        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(awtFont, portableImages)
        if (backgroundColor != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, image.width, image.height)
        }
        if (extraPadding != 0) {
            gfx.translate(extraPadding, extraPadding)
        }
        gfx.color = Color(textColor)
        var y = fontMetrics.ascent.toFloat()
        val dy = texture.height.toFloat()
        for (yi in simpleChars.indices) {
            // not necessary on desktop, but improves quality on Android, because mono somehow is not mono :)
            val width = TextLayout(simpleChars[yi], gfx.font, renderContext).bounds.maxX.toFloat()
            gfx.drawString(simpleChars[yi], (texture.width - width) * 0.5f, y)
            y += dy
        }
        gfx.dispose()
        texture.create(image.toImage(), sync = true)
    }

    override fun toString(): String {
        return font.toString()
    }

    companion object {

        private fun getStringWidth(group: TextGroup) = group.offsets.last() - group.offsets.first()
        private fun createGroup(font: me.anno.fonts.Font, text: CharSequence): TextGroup = TextGroup(font, text, 0.0)

        private val asciiStrings = Array(128) { it.toChar().toString() }

        private val splittingOrder: List<Collection<Int>> = listOf(
            listOf(' '.code),
            listOf('-'.code),
            listOf('/', '\\', ':', '-', '*', '?', '=', '&', '|', '!', '#').map { it.code }.toSortedSet(),
            listOf(','.code, '.'.code)
        )

        private val fallbackFontList = DefaultConfig[
            "ui.font.fallbacks",
            "Segoe UI Emoji,Segoe UI Symbol,DejaVu Sans,FreeMono,Unifont,Symbola"
        ].split(',').mapNotNull { if (it.isBlank2()) null else it.trim() }

        private val fallbackFonts = HashMap<Float, List<AWTFont>>()
        private fun getFallback(size: Float): List<AWTFont> {
            val cached = fallbackFonts[size]
            if (cached != null) return cached
            val fonts = fallbackFontList.mapNotNull {
                FontManager.getFont(it, size, bold = false, italic = false) as? AWTFont
            }
            fallbackFonts[size] = fonts
            return fonts
        }
    }
}