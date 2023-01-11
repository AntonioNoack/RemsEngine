package me.anno.fonts

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.clamp
import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.OS
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.structures.lists.ExpensiveList
import me.anno.utils.types.Strings.incrementTab
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.streams.toList

class AWTFont(val font: Font) {

    val name: String get() = font.name
    val size get() = font.size
    val style get() = font.style

    private val fontMetrics = if (OS.isWeb) {
        WebFonts.getFontMetrics(font)
    } else {
        val unused = BufferedImage(1, 1, 1).graphics as Graphics2D
        unused.prepareGraphics(font, false)
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

    private fun getStringWidth(group: TextGroup) = group.offsets.last() - group.offsets.first()

    private fun getGroup(text: CharSequence) = TextGroup(this, text, 0.0)

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
        val group2 = group ?: getGroup(text)
        // some distances still are awkward, because it is using the closest position, not float
        // (useful for "I"s)
        // maybe we could implement detecting, which sections need int positions, and which don't...
        if (text.containsSpecialChar()) {
            var index = 0
            for (codepoint in text.codePoints()) {
                // println("  drawing $codepoint = '${String(Character.toChars(codepoint))}' at ${x + group2.offsets[index].toFloat()}, $y")
                gfx.drawString(
                    String(Character.toChars(codepoint)),
                    x + group2.offsets[index].toFloat(), y
                )
                index++
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

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToInt()

    fun calculateSize(text: CharSequence, fontSize: Float, widthLimit: Int, heightLimit: Int): Int {
        if (text.isEmpty()) return GFXx2D.getSize(0, fontSize.toInt())
        return if (text.containsSpecialChar() || (widthLimit in 0 until GFX.maxTextureSize)) {
            val parts = splitParts(
                text, fontSize, 4f, 0f,
                widthLimit.toFloat(), heightLimit.toFloat()
            )
            val width = min(ceil(parts.width).toInt(), widthLimit)
            val height = min(ceil(parts.height).toInt(), heightLimit)
            return GFXx2D.getSize(width, height)
        } else {
            val baseWidth = getStringWidth(getGroup(text))
            val width = clamp(baseWidth.roundToInt() + 1, 0, GFX.maxTextureSize)
            val height = min(fontMetrics.height, GFX.maxTextureSize)
            GFXx2D.getSize(width, height)
        }
    }

    fun generateTexture(
        text: CharSequence,
        fontSize: Float,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        textColor: Int = -1,
        backgroundColor: Int = 255 shl 24,
        extraPadding: Int = 0
    ): ITexture2D? {

        if (text.isEmpty()) return null
        if (text.containsSpecialChar() || widthLimit < text.length * fontSize * 2f) {
            return generateTextureV3(
                text, fontSize, widthLimit.toFloat(), heightLimit.toFloat(), portableImages,
                textColor, backgroundColor, extraPadding
            )
        }

        val group = getGroup(text)
        val width = min(widthLimit, getStringWidth(group).roundToInt() + 1 + 2 * extraPadding)

        val lineCount = 1
        val fontHeight = fontMetrics.height
        val height = min(heightLimit, fontHeight * lineCount + 2 * extraPadding)

        if (width < 1 || height < 1) return null
        if (max(width, height) > GFX.maxTextureSize) {
            IllegalArgumentException(
                "Texture for text is too large! $width x $height > ${GFX.maxTextureSize}, " +
                        "${text.length} chars, $lineCount lines, ${font.name} $fontSize px, ${
                            text.toString().shorten(200)
                        }"
            ).printStackTrace()
            return null
        }

        if (text.isBlank2()) {
            // we need some kind of wrapper around texture2D
            // and return an empty/blank texture
            // that the correct size is returned is required by text input fields
            // (with whitespace at the start or end)
            return FakeWhiteTexture(width, height)
        }

        val texture = Texture2D("awt-" + text.shorten(24), width, height, 1)
        val prio = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        if (prio) {
            createImage(
                texture,
                width, height, portableImages,
                textColor, backgroundColor, extraPadding, text, group
            )
        } else {
            GFX.addGPUTask("awt-font-v5", width, height) {
                createImage(
                    texture,
                    width, height, portableImages,
                    textColor, backgroundColor, extraPadding, text, group
                )
            }
        }

        return texture

    }

    private fun createImage(
        texture: Texture2D,
        width: Int, height: Int,
        portableImages: Boolean,
        textColor: Int, backgroundColor: Int,
        extraPadding: Int, text: CharSequence, group: TextGroup?
    ) {
        val image = BufferedImage(width, height, 1)
        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(font, portableImages)

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
        if (debugJVMResults) debug(image)
        texture.create(image, sync = true, checkRedundancy = false)?.invoke()
    }

    fun debug(image: BufferedImage) {
        OS.desktop.getChild("img").tryMkdirs()
        OS.desktop.getChild("img/${ctr++}.png").outputStream().use {
            ImageIO.write(image, "png", it)
        }
    }

    private val renderContext by lazy {
        FontRenderContext(null, true, true)
    }

    val exampleLayout by lazy {
        TextLayout("o", font, renderContext)
    }

    val actualFontSize by lazy {
        exampleLayout.ascent + exampleLayout.descent
    }

    @Suppress("unused")
    val ascent by lazy { exampleLayout.ascent }

    @Suppress("unused")
    val descent by lazy { exampleLayout.descent }

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

        fonts += AWTFont(font)
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

    fun getSupportLevel(fonts: List<AWTFont>, char: Int, lastSupportLevel: Int): Int {
        for (index in fonts.indices) {
            val font = fonts[index]
            if (font.font.canDisplay(char)) return index
        }
        return lastSupportLevel
    }

    private fun findSplitIndex(
        chars: List<Int>, index0: Int, index1: Int,
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
                val advance2 = TextLayout(substring2.toString(), font, renderContext).advance
                advance2 + (splitIndex - index0) * charSpacing // width
            }

            val delta = lineBreakWidth - currentX
            var lastValidSplitIndex = listOfWidths.binarySearch { it.compareTo(delta) }
            if (lastValidSplitIndex < 0) lastValidSplitIndex = -1 - lastValidSplitIndex
            lastValidSplitIndex = max(0, lastValidSplitIndex - 1)

            val charsOfInterest = chars.subList(firstSplitIndex, firstSplitIndex + lastValidSplitIndex + 1)
            var foundSolution = false
            search@ for (splittingChars in splittingOrder) {
                for ((index, char) in charsOfInterest.withIndex().reversed()) {
                    if (char in splittingChars) {
                        // found the best splitting char <3
                        lastValidSplitIndex = min(index + 1, lastValidSplitIndex)
                        foundSolution = true
                        break@search
                    }
                }
            }

            if (!foundSolution) {// prefer to split upper case characters
                search@ for ((index, charV) in charsOfInterest.withIndex().reversed()) {
                    val char = charV.toChar()
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

        val chars = line.codePoints().toList()
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
                        TextLayout(filtered.toString(), font.font, renderContext).advance else 0f
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
                        result += StringPart(currentX, currentY, chars.joinChars(index0, index1), font, 0f)
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
        widthLimit: Float,
        heightLimit: Float,
        portableImages: Boolean,
        textColor: Int,
        backgroundColor: Int,
        extraPadding: Int
    ): ITexture2D {

        val parts = splitParts(text, fontSize, 4f, 0f, widthLimit, heightLimit)
        val result = parts.parts

        val width = min(ceil(parts.width + 2 * extraPadding), widthLimit).toInt()
        val height = min(ceil(parts.height + 2 * extraPadding), heightLimit).toInt()

        if (result.isEmpty() || width < 1 || height < 1) return FakeWhiteTexture(width, height)

        val texture = Texture2D("awt-font-v3", width, height, 1)
        val prio = GFX.isGFXThread() && (GFX.loadTexturesSync.peek() || text.length == 1)
        if (prio) {
            createImage(texture, width, height, portableImages, textColor, backgroundColor, extraPadding, result)
        } else {
            GFX.addGPUTask("awt-font-v6", width, height) {
                createImage(texture, width, height, portableImages, textColor, backgroundColor, extraPadding, result)
            }
        }
        return texture
    }

    private fun createImage(
        texture: Texture2D, width: Int, height: Int, portableImages: Boolean,
        textColor: Int, backgroundColor: Int, extraPadding: Int, result: List<StringPart>
    ) {
        val image = BufferedImage(width, height, 1)
        // for (i in width-10 until width) image.setRGB(i, 0, 0xff0000)

        val gfx = image.graphics as Graphics2D
        gfx.prepareGraphics(font, portableImages)
        gfx.background = Color(backgroundColor)
        if (backgroundColor.and(0xffffff) != 0) {
            // fill background with that color
            gfx.color = Color(backgroundColor)
            gfx.fillRect(0, 0, width, height)
        }
        gfx.translate(extraPadding, extraPadding)
        gfx.color = Color(textColor)

        val y = exampleLayout.ascent

        for (s in result) {
            val font = s.font
            gfx.font = font.font
            // println("drawing string ${s.text} by layout at ${s.xPos}, ${s.yPos} + $y")
            font.drawString(gfx, s.text, null, s.xPos, s.yPos + y)
        }

        gfx.dispose()
        if (debugJVMResults) debug(image)
        texture.create(image, sync = true, checkRedundancy = false)?.invoke()
    }

    companion object {

        val asciiStrings = Array(128) { it.toChar().toString() }

        val splittingOrder: List<Collection<Int>> = listOf(
            listOf(' '.code),
            listOf('-'.code),
            listOf('/', '\\', ':', '-', '*', '?', '=', '&', '|', '!', '#').map { it.code }.toSortedSet(),
            listOf(','.code, '.'.code)
        )

        const val debugJVMResults = false

        var ctr = 0

        // val staticGfx = BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D
        // val staticMetrics = staticGfx.fontMetrics
        // val staticFontRenderCTX = staticGfx.fontRenderContext

        private val fallbackFontList = DefaultConfig[
                "ui.font.fallbacks",
                "Segoe UI Emoji,Segoe UI Symbol,DejaVu Sans,FreeMono,Unifont,Symbola"
        ].split(',').mapNotNull { if (it.isBlank2()) null else it.trim() }

        // val fallbacks = FontManager.getFont("", size, 0f, 0f)
        // var fallbackFont0 = Font("Segoe UI Emoji", Font.PLAIN, 25)
        private val fallbackFonts = HashMap<Float, List<AWTFont>>()
        fun getFallback(size: Float): List<AWTFont> {
            val cached = fallbackFonts[size]
            if (cached != null) return cached
            val fonts = fallbackFontList.map { FontManager.getFont(it, size, bold = false, italic = false) }
            fallbackFonts[size] = fonts
            return fonts
        }
    }
}