package me.anno.fonts

import me.anno.config.DefaultConfig
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.OS
import me.anno.utils.files.Files.use
import me.anno.utils.structures.lists.ExpensiveList
import me.anno.utils.types.Lists.join
import me.anno.utils.types.Strings.incrementTab
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.streams.toList

class AWTFont(val font: Font) {

    private val fontMetrics: FontMetrics

    init {
        val unused = BufferedImage(1, 1, 1).graphics as Graphics2D
        unused.prepareGraphics(font)
        fontMetrics = unused.fontMetrics
    }

    private fun String.containsSpecialChar(): Boolean {
        val limit = 127.toChar()
        for (ci in indices) {
            val cp = get(ci)
            if (cp == '\n' || cp == '\t' || cp > limit) return true
        }
        return false
    }

    private fun String.countLines() = count { it == '\n' } + 1

    private fun getStringWidth(group: TextMeshGroup) = group.offsets.last() - group.offsets.first()

    private fun getGroup(text: String) = TextMeshGroup(font, text, 0f, true)

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, text: String, group: TextMeshGroup?, y: Int) =
        drawString(gfx, text, group, 0f, y.toFloat())

    /**
     * like gfx.drawText, however this method is respecting the ideal character distances,
     * so there are no awkward spaces between T and e
     * */
    private fun drawString(gfx: Graphics2D, text: String, group: TextMeshGroup?, x: Float, y: Float) {
        val group2 = group ?: getGroup(text)
        // some distances still are awkward, because it is using the closest position, not float
        // (useful for 'I's)
        // maybe we could implement detecting, which sections need int positions, and which don't...
        if (text.containsSpecialChar()) {
            var index = 0
            for (codepoint in text.codePoints()) {
                gfx.drawString(
                    String(Character.toChars(codepoint)),
                    x + group2.offsets[index].toFloat(),
                    y
                )
                index++
            }
        } else {
            for (index in text.indices) {
                val char = text[index]
                gfx.drawString(
                    asciiStrings[char.code],
                    x + group2.offsets[index].toFloat(),
                    y
                )
            }
        }
    }

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToInt()

    fun calculateSize(text: String, fontSize: Float, widthLimit: Int, heightLimit: Int): Int {

        if (text.isEmpty()) return GFXx2D.getSize(0, fontSize.toInt())
        if (text.containsSpecialChar() || widthLimit > 0) {
            return generateSizeV3(text, fontSize, widthLimit.toFloat(), heightLimit.toFloat())
        }

        val lines = text.split('\n')
        val lineCount = lines.size
        val spaceBetweenLines = spaceBetweenLines(fontSize)
        val fontHeight = fontMetrics.height
        val height = fontHeight * lineCount + (lineCount - 1) * spaceBetweenLines

        val width = max(0, lines.maxOf { getStringWidth(getGroup(it)) }.roundToInt() + 1)
        return GFXx2D.getSize(width, height)

    }

    fun generateTexture(text: String, fontSize: Float, widthLimit: Int, heightLimit: Int): ITexture2D? {

        if (text.isEmpty()) return null
        if (text.containsSpecialChar() || widthLimit > 0) {
            return generateTextureV3(text, fontSize, widthLimit.toFloat(), heightLimit.toFloat())
        }

        val group = getGroup(text)
        val width = getStringWidth(group).roundToInt() + 1

        val lineCount = text.countLines()
        val spaceBetweenLines = spaceBetweenLines(fontSize)
        val fontHeight = fontMetrics.height
        val height = fontHeight * lineCount + (lineCount - 1) * spaceBetweenLines

        if (width < 1 || height < 1) return null
        if (text.isBlank2()) {
            // we need some kind of wrapper around texture2D
            // and return an empty/blank texture
            // that the correct size is returned is required by text input fields
            // (with whitespace at the start or end)
            return FakeWhiteTexture(width, height)
        }

        val texture = Texture2D("awt-font", width, height, 1)
        texture.create2("AWTFont.generateTexture", {

            val image = BufferedImage(width, height, 1)

            val gfx = image.graphics as Graphics2D
            gfx.prepareGraphics(font)

            val y = fontMetrics.ascent

            if (lineCount == 1) {
                drawString(gfx, text, group, y)
            } else {
                val lines = text.split('\n')
                lines.forEachIndexed { index, line ->
                    drawString(gfx, line, null, y + index * (fontHeight + spaceBetweenLines))
                }
            }
            gfx.dispose()
            if (debugJVMResults) debug(image)
            image

        }, needsSync, true)

        return texture

    }

    fun debug(image: BufferedImage) {
        OS.desktop.getChild("img").mkdirs()
        use(OS.desktop.getChild("img/${ctr++}.png").outputStream()) {
            ImageIO.write(image, "png", it)
        }
    }

    private val renderContext by lazy {
        FontRenderContext(null, true, true)
    }

    private val exampleLayout by lazy {
        TextLayout("o", font, renderContext)
    }

    val actualFontSize by lazy {
        exampleLayout.ascent + exampleLayout.descent
    }

    private fun isSpace(char: Int) = char == '\t'.code || char == ' '.code

    fun splitParts(
        text: String,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        lineBreakWidth: Float,
        textBreakHeight: Float
    ): PartResult {

        val fallback = getFallback(fontSize)
        val fonts = ArrayList<Font>(fallback.size + 1)

        fonts += font
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
        val parts = splitLines.mapNotNull { partResult ->
            val offsetY = actualFontSize * lineCount
            lineCount += partResult?.lineCount ?: 1
            val offset = Vector2f(0f, offsetY)
            partResult?.parts?.map { it + offset }
            // todo break, if the line limit has been reached
        }.join()
        val height = lineCount * actualFontSize
        return PartResult(parts, width, height, lineCount, exampleLayout)
    }

    fun getSupportLevel(fonts: List<Font>, char: Int, lastSupportLevel: Int): Int {
        for (index in fonts.indices) {
            val font = fonts[index]
            if (font.canDisplay(char)) return index
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
                val substring2 = chars.subList(index0, splitIndex).joinChars()
                val advance2 = TextLayout(substring2, font, renderContext).advance
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
        fonts: List<Font>,
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
            if (index1 > index0) {
                val substring = chars.subList(index0, index1).joinChars()
                val font = fonts[lastSupportLevel]
                val layout = TextLayout(substring, font, renderContext)
                val advance = layout.advance
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
                    display()
                } else {
                    result += StringPart(currentX, currentY, substring, font, 0f)
                    currentX = nextX
                    widthF = max(widthF, currentX)
                    index0 = index1
                }
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
        return PartResult(result, widthF, currentY, lineCount, exampleLayout)

    }

    private fun generateSizeV3(text: String, fontSize: Float, lineBreakWidth: Float, textBreakHeight: Float): Int {

        val parts = splitParts(text, fontSize, 4f, 0f, lineBreakWidth, textBreakHeight)

        val width = ceil(parts.width).toInt()
        val height = ceil(parts.height).toInt()

        return GFXx2D.getSize(width, height)

    }

    private fun generateTextureV3(
        text: String,
        fontSize: Float,
        lineBreakWidth: Float,
        textBreakHeight: Float
    ): ITexture2D {

        val parts = splitParts(text, fontSize, 4f, 0f, lineBreakWidth, textBreakHeight)
        val result = parts.parts
        val exampleLayout = parts.exampleLayout

        val width = ceil(parts.width).toInt()
        val height = ceil(parts.height).toInt()

        if (result.isEmpty() || width < 1 || height < 1) return FakeWhiteTexture(width, height)

        val texture = Texture2D("awt-font-v3", width, height, 1)
        texture.create2("AWTFont.generateTextureV3", {

            val image = BufferedImage(width, height, 1)
            // for (i in width-10 until width) image.setRGB(i, 0, 0xff0000)

            val gfx = image.graphics as Graphics2D
            gfx.prepareGraphics(font)

            val x = (image.width - width) * 0.5f
            val y = (image.height - height) * 0.5f + exampleLayout.ascent

            result.forEach {
                gfx.font = it.font
                drawString(gfx, it.text, null, it.xPos + x, it.yPos + y)
            }

            gfx.dispose()
            if (debugJVMResults) debug(image)

            image

        }, needsSync, true)

        return texture

    }

    companion object {

        val asciiStrings = Array(128) { it.toChar().toString() }

        val splittingOrder: List<Collection<Int>> = listOf(
            listOf(' ').map { it.code },
            listOf('-').map { it.code },
            listOf('/', '\\', ':', '-', '*', '?', '=', '&', '|', '!', '#').map { it.code }.toSortedSet(),
            listOf(',', '.').map { it.code }
        )

        // I get pixel errors with running on multiple threads
        // (java 1.8.0 build 112, windows 10, 64 bit)
        // this should be investigated for other Java versions and on Linux...
        val isJVMImplementationThreadSafe = false
        val needsSync = !isJVMImplementationThreadSafe
        const val debugJVMResults = false

        var ctr = 0

        val LOGGER = LogManager.getLogger(AWTFont::class)!!

        // val staticGfx = BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D
        // val staticMetrics = staticGfx.fontMetrics
        // val staticFontRenderCTX = staticGfx.fontRenderContext

        private val fallbackFontList = DefaultConfig[
                "ui.font.fallbacks",
                "Segoe UI Emoji,Segoe UI Symbol,DejaVu Sans,FreeMono,Unifont,Symbola"
        ]
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // val fallbacks = FontManager.getFont("", size, 0f, 0f)
        // var fallbackFont0 = Font("Segoe UI Emoji", Font.PLAIN, 25)
        private val fallbackFonts = HashMap<Float, List<Font>>()
        fun getFallback(size: Float): List<Font> {
            val cached = fallbackFonts[size]
            if (cached != null) return cached
            val fonts = fallbackFontList
                .map { FontManager.getFont(it, size, false, false).font }
            fallbackFonts[size] = fonts
            return fonts
        }
    }
}