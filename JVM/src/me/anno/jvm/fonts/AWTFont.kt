package me.anno.jvm.fonts

import me.anno.config.DefaultConfig
import me.anno.fonts.Font
import me.anno.fonts.FontImpl
import me.anno.image.raw.IntImage
import me.anno.jvm.fonts.DefaultRenderingHints.prepareGraphics
import me.anno.maths.Maths.fract
import me.anno.utils.Color.g
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

object AWTFont : FontImpl<List<FontData>>() {

    override fun getBaselineY(font: Font): Float {
        return FontManagerImpl.getAWTFont(font).baselineY
    }

    override fun getFallbackFonts(font: Font): List<FontData> {
        return (listOf(font.name) + fallbackFontList).map { newName ->
            FontManagerImpl.getAWTFont(font.withName(newName))
        }
    }

    override fun getLineHeight(font: Font): Float {
        return FontManagerImpl.getAWTFont(font).lineHeight
    }

    override fun getTextLength(font: Font, codepoint: Int): Int {
        return FontManagerImpl.getTextLength1(font, codepoint)
    }

    override fun getTextLength(font: Font, codepointA: Int, codepointB: Int): Int {
        return FontManagerImpl.getTextLength2(font, codepointA, codepointB)
    }

    override fun drawGlyph(
        image: IntImage,
        x0: Int, x1: Int, y0: Int, y1: Int, strictBounds: Boolean,
        font: Font, fallbackFonts: List<FontData>, fontIndex: Int,
        codepoint: Int, textColor: Int, backgroundColor: Int,
        portableImages: Boolean
    ) {

        // todo cache the value if it makes sense... discretize fract(x0) and fract(y0) reasonably

        val fontData = fallbackFonts[fontIndex]
        val tmp = BufferedImage(max(x1 - x0 + 1, 1), y1 - y0, BI_FORMAT)
        val gfx = tmp.graphics as Graphics2D
        gfx.prepareGraphics(portableImages)

        gfx.background = Color.BLACK
        gfx.color = Color.WHITE

        val text = if (codepoint in 0 until 128) asciiStrings[codepoint] else codepoint.joinChars()
        gfx.font = fontData.awtFont
        gfx.drawString(text, 0f, fontData.baselineY)
        gfx.dispose()

        for (yi in 0 until tmp.height) {
            for (xi in 0 until tmp.width) {
                val color = tmp.getRGB(xi, yi)
                if (color.g() == 0) continue
                image.setRGB(x0 + xi, y0 + yi, color)
            }
        }
    }

    override fun getSupportLevel(fonts: List<FontData>, codepoint: Int, lastSupportLevel: Int): Int {
        for (index in fonts.indices) {
            val font = fonts[index]
            if (font.awtFont.canDisplay(codepoint)) {
                return index
            }
        }

        LOGGER.warn("Glyph '$codepoint' cannot be displayed")
        return lastSupportLevel
    }

    private val LOGGER = LogManager.getLogger(AWTFont::class)

    /**
     * Must not have alpha channel for subpixel-data!
     * We render our text-textures the following way:
     *  - text has alpha = 0 (alpha is unsupported anyway)
     *  - emojis have alpha >= 0
     * */
    const val BI_FORMAT = BufferedImage.TYPE_INT_RGB

    private val asciiStrings = List(128) { it.toChar().toString() }

    private val fallbackFontList = DefaultConfig[
        "ui.font.fallbacks",
        "Segoe UI Emoji,Segoe UI Symbol,DejaVu Sans,FreeMono,Unifont,Symbola"
    ].split(',').mapNotNull { if (it.isBlank2()) null else it.trim() }
}