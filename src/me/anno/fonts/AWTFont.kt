package me.anno.fonts

import me.anno.config.DefaultConfig
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.*
import me.anno.objects.Text
import me.anno.ui.base.DefaultRenderingHints.prepareGraphics
import me.anno.utils.OS
import me.anno.utils.StringHelper.incrementTab
import me.anno.utils.StringHelper.joinChars
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.io.File
import java.lang.StrictMath.round
import javax.imageio.ImageIO
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
            if (cp > 127 || cp == '\t'.toInt()) return true
        }
        return false
    }

    fun String.countLines() = count { it == '\n' } + 1

    // todo move to generating the texture on the gpu with our triangulated meshes...
    fun generateTextureV4(text: String, fontSize: Float, widthLimit: Int): ITexture2D? {

        val textElement = Text(text)

        val w = (fontSize * 3).toInt()
        val h = fontSize.toInt()
        val fb = Framebuffer("awt-font-v4", w, h, 8, 1, true, Framebuffer.DepthBufferType.NONE)

        Frame(w, h, true, fb) {
            textElement.draw(Matrix4fArrayList(), 0.0, Vector4f(1f))
        }

        fb.bindTexture0(0, GPUFiltering.NEAREST, Clamping.CLAMP)
        // todo destroy all but texture0
        return fb.msBuffer!!.textures[0]

    }

    fun generateTexture(text: String, fontSize: Float, widthLimit: Int): ITexture2D? {

        // return generateTextureV4(text, fontSize, widthLimit)

        if (text.isEmpty()) return null
        if (containsSpecialChar(text) || widthLimit > 0) {
            return generateTextureV3(text, fontSize, widthLimit.toFloat())
        }

        val width = fontMetrics.stringWidth(text) + (if (font.isItalic) max(2, (fontSize / 5f).roundToInt()) else 1)
        val lineCount = text.countLines()
        val spaceBetweenLines = (0.5f * fontSize).roundToInt()
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
        texture.create({

            val image = BufferedImage(width, height, 1)
            val gfx = image.graphics as Graphics2D
            gfx.prepareGraphics(font)

            val x = 0
            val y = fontMetrics.ascent

            if (lineCount == 1) {
                gfx.drawString(text, x, y)
            } else {
                val lines = text.split('\n')
                lines.forEachIndexed { index, line ->
                    gfx.drawString(line, x, y + index * (fontHeight + spaceBetweenLines))
                }
            }
            gfx.dispose()
            if (debugJVMResults) debug(image)
            image

        }, needsSync)

        return texture

    }

    fun debug(image: BufferedImage) {
        ImageIO.write(image, "png", File(OS.desktop, "img/${ctr++}.png"))
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

        return PartResult(result, widthF, currentY, exampleLayout)

    }

    fun generateTextureV3(text: String, fontSize: Float, lineBreakWidth: Float): Texture2D? {

        val parts = splitParts(text, fontSize, 4f, 0f, lineBreakWidth)
        val result = parts.parts
        val exampleLayout = parts.exampleLayout

        val width = ceil(parts.width)
        val height = ceil(parts.height)

        val texture = Texture2D("awt-font-v3", width, height, 1)
        texture.create({
            val image = BufferedImage(width, height, 1)
            if (result.isNotEmpty()) {
                val gfx = image.graphics as Graphics2D
                gfx.prepareGraphics(font)

                val x = (image.width - width) * 0.5f
                val y = (image.height - height) * 0.5f + exampleLayout.ascent

                result.forEach {
                    gfx.font = it.font
                    gfx.drawString(it.text, it.xPos + x, it.yPos + y)
                }

                gfx.dispose()
                if (debugJVMResults) debug(image)
            }
            image
        }, needsSync)

        return texture

    }

    fun ceil(f: Float) = round(f + 0.5f)
    fun ceil(f: Double) = round(f + 0.5).toInt()

    companion object {

        // I get pixel errors with running on multiple threads
        // (java 1.8.0 build 112, windows 10, 64 bit)
        // this should be investigated for other Java versions and on Linux...
        val isJVMImplementationThreadSafe = false
        val needsSync = !isJVMImplementationThreadSafe
        val debugJVMResults = false

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