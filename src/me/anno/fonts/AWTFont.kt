package me.anno.fonts

import me.anno.config.DefaultConfig
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.OS
import me.anno.utils.StringHelper.incrementTab
import me.anno.utils.StringHelper.joinChars
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.streams.toList

class AWTFont(val font: Font) {

    private val fontMetrics: FontMetrics

    init {
        val unused = BufferedImage(1, 1, 1).graphics as Graphics2D
        unused.prepareGraphics(font)
        fontMetrics = unused.fontMetrics
    }

    fun containsSpecialChar(text: String): Boolean {
        for (cp in text.codePoints()) {
            if (cp == '\n'.toInt() || cp > 127 || cp == '\t'.toInt()) return true
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
        var index = 0
        // some distances still are awkward, because it is using the closest position, not float
        // (useful for 'I's)
        // maybe we could implement detecting, which sections need int positions, and which don't...
        text.codePoints().forEach { codepoint ->
            gfx.drawString(
                String(Character.toChars(codepoint)),
                x + group2.offsets[index].toFloat(),
                y
            )
            index++
        }
    }

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToInt()

    fun calculateSize(text: String, fontSize: Float, widthLimit: Int): Pair<Int, Int> {

        if (text.isEmpty()) return 0 to fontSize.toInt()
        if (containsSpecialChar(text) || widthLimit > 0) {
            return generateSizeV3(text, fontSize, widthLimit.toFloat())
        }

        val lines = text.split('\n')
        val lineCount = lines.size
        val spaceBetweenLines = spaceBetweenLines(fontSize)
        val fontHeight = fontMetrics.height
        val height = fontHeight * lineCount + (lineCount - 1) * spaceBetweenLines

        val width = max(0, lines.map { getStringWidth(getGroup(it)) }.max()!!.roundToInt() + 1)
        return width to height

    }

    fun generateTexture(text: String, fontSize: Float, widthLimit: Int): ITexture2D? {

        if (text.isEmpty()) return null
        if (containsSpecialChar(text) || widthLimit > 0) {
            return generateTextureV3(text, fontSize, widthLimit.toFloat())
        }

        val group = getGroup(text)
        val width = getStringWidth(group).roundToInt() + 1

        val lineCount = text.countLines()
        val spaceBetweenLines = spaceBetweenLines(fontSize)
        val fontHeight = fontMetrics.height
        val height = fontHeight * lineCount + (lineCount - 1) * spaceBetweenLines

        if (width < 1 || height < 1) return null
        if (text.isBlank()) {
            // we need some kind of wrapper around texture2D
            // and return an empty/blank texture
            // that the correct size is returned is required by text input fields
            // (with whitespace at the start or end)
            return FakeWhiteTexture(width, height)
        }

        val texture = Texture2D("awt-font", width, height, 1)
        texture.create("AWTFont.generateTexture", {

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

        }, needsSync)

        return texture

    }

    fun debug(image: BufferedImage) {
        val parent = File(OS.desktop, "img")
        parent.mkdir()
        ImageIO.write(image, "png", File(parent, "${ctr++}.png"))
    }

    val renderContext by lazy {
        FontRenderContext(null, true, true)
    }

    val exampleLayout by lazy {
        TextLayout("o", font, renderContext)
    }

    val actualFontSize by lazy {
        exampleLayout.ascent + exampleLayout.descent
    }

    fun splitParts(
        text: String,
        fontSize: Float,
        relativeTabSize: Float,
        relativeCharSpacing: Float,
        lineBreakWidth: Float
    ): PartResult {

        val fallback = getFallback(fontSize)
        val fonts = ArrayList<Font>(fallback.size + 1)

        fonts += font
        fonts += fallback

        fun getSupportLevel(char: Int, lastSupportLevel: Int): Int {
            fonts.forEachIndexed { index, font ->
                if (font.canDisplay(char)) return index
            }
            return lastSupportLevel
        }

        val hasAutomaticLineBreak = lineBreakWidth >= 0f
        val lines = text.split('\n')
        val result = ArrayList<StringPart>(lines.size * 2)
        val tabSize = exampleLayout.advance * relativeTabSize
        val charSpacing = fontSize * relativeCharSpacing
        var widthF = 0f
        var currentX = 0f
        var currentY = 0f
        val fontHeight = actualFontSize
        var startResultIndex = 0
        lines.forEach { line ->
            val cp = line.codePoints().toList()
            var startIndex = 0
            var index = 0
            var lastSupportLevel = 0
            fun display() {
                if (index > startIndex) {
                    val substring = cp.subList(startIndex, index).joinChars()
                    val font = fonts[lastSupportLevel]
                    val layout = TextLayout(substring, font, renderContext)
                    // val bounds = layout.bounds
                    result += StringPart(currentX, currentY, substring, font, 0f)
                    currentX += layout.advance + (index - startIndex) * charSpacing
                    widthF = max(widthF, currentX)
                    startIndex = index
                }
            }

            fun nextLine() {
                display()
                for (i in startResultIndex until result.size) {
                    result[i].lineWidth = max(0f, currentX - charSpacing)
                }
                startResultIndex = result.size
                currentY += fontHeight
                currentX = 0f
            }

            fun isSpace(char: Int) = char == '\t'.toInt() || char == ' '.toInt()
            var hadNonSpaceCharacter = false
            while (index < cp.size) {
                when (val char = cp[index]) {
                    '\t'.toInt() -> {
                        display()
                        startIndex++ // skip \t too
                        currentX = incrementTab(currentX, tabSize, relativeTabSize)
                    }
                    ' '.toInt() -> {

                        // break line, if the next work doesn't fit in this line, and there already was a word
                        // search for the next word
                        if (hasAutomaticLineBreak && index + 1 < cp.size && !isSpace(cp[index + 1]) && hadNonSpaceCharacter) {

                            var endIndex = index + 1
                            while (endIndex < cp.size) {
                                if (!isSpace(cp[endIndex])) endIndex++
                                else break
                            }

                            // not 100% accurate for text with smileys
                            val previousWord = cp.subList(startIndex, index).joinChars()
                            val nextWord = cp.subList(index + 1, endIndex).joinChars()
                            val currentX2 = currentX +
                                    if (previousWord.isEmpty()) 0f
                                    else TextLayout(previousWord, font, renderContext).advance
                            val layout = TextLayout(nextWord, font, renderContext)
                            val advance = layout.advance
                            if (currentX2 + advance + (endIndex - startIndex) * charSpacing > lineBreakWidth) {
                                // it doesn't fit -> line break
                                hadNonSpaceCharacter = false
                                nextLine()
                                startIndex++
                            }

                        }

                        // todo break very long words by force (on sense making syllables)
                        val supportLevel = getSupportLevel(char, lastSupportLevel)
                        if (supportLevel != lastSupportLevel) {
                            display()
                            lastSupportLevel = supportLevel
                        }

                    }
                    else -> {
                        hadNonSpaceCharacter = true
                        val supportLevel = getSupportLevel(char, lastSupportLevel)
                        if (supportLevel != lastSupportLevel) {
                            display()
                            lastSupportLevel = supportLevel
                        }
                    }
                }
                index++
            }
            nextLine()
        }

        //println("$text -> ${result.joinToString { it.text }}")
        return PartResult(result, widthF, currentY, exampleLayout)

    }

    fun generateSizeV3(text: String, fontSize: Float, lineBreakWidth: Float): Pair<Int, Int> {

        val parts = splitParts(text, fontSize, 4f, 0f, lineBreakWidth)

        val width = ceil(parts.width).toInt()
        val height = ceil(parts.height).toInt()

        return width to height

    }

    fun generateTextureV3(text: String, fontSize: Float, lineBreakWidth: Float): ITexture2D? {

        val parts = splitParts(text, fontSize, 4f, 0f, lineBreakWidth)
        val result = parts.parts
        val exampleLayout = parts.exampleLayout

        val width = ceil(parts.width).toInt()
        val height = ceil(parts.height).toInt()

        if (result.isEmpty()) return FakeWhiteTexture(width, height)

        val texture = Texture2D("awt-font-v3", width, height, 1)
        texture.create("AWTFont.generateTextureV3", {

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

        }, needsSync)

        return texture

    }

    companion object {

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