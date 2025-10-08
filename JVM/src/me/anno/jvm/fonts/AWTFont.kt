package me.anno.jvm.fonts

import me.anno.config.DefaultConfig
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

object AWTFont : FontImpl<List<FontData>>() {

    override fun getBaselineY(font: me.anno.fonts.Font): Float {
        return FontManagerImpl.getAWTFont(font).baselineY
    }

    override fun getFallbackFonts(font: me.anno.fonts.Font): List<FontData> {
        return (listOf(font.name) + fallbackFontList).map { newName ->
            FontManagerImpl.getAWTFont(font.withName(newName))
        }
    }

    override fun getLineHeight(font: me.anno.fonts.Font): Float {
        return FontManagerImpl.getAWTFont(font).lineHeight
    }

    override fun getTextLength(font: me.anno.fonts.Font, codepoint: Int): Float {
        return FontManagerImpl.getTextLength1(font, codepoint).toFloat()
    }

    override fun getTextLength(font: me.anno.fonts.Font, codepointA: Int, codepointB: Int): Float {
        return FontManagerImpl.getTextLength2(font, codepointA, codepointB).toFloat()
    }

    override fun drawGlyph(
        image: IntImage,
        x0: Float, x1: Float, y0: Float, y1: Float, strictBounds: Boolean,
        font: me.anno.fonts.Font, fallbackFonts: List<FontData>, fontIndex: Int,
        codepoint: Int, textColor: Int, backgroundColor: Int, portableImages: Boolean
    ) {

        // todo cache the value if it makes sense... discretize fract(x0) and fract(y0) reasonably

        val fontData = fallbackFonts[fontIndex]
        val tmp = BufferedImage((ceil(x1) - floor(x0)).toIntOr(), (y1 - y0).toIntOr(), BI_FORMAT)
        val gfx = tmp.graphics as Graphics2D
        gfx.prepareGraphics(portableImages)

        gfx.background = Color.BLACK
        gfx.color = Color.WHITE

        val text = if (codepoint in 0 until 128) asciiStrings[codepoint] else codepoint.joinChars()
        gfx.font = fontData.awtFont
        gfx.drawString(text, fract(x0), fontData.baselineY + fract(y0))
        gfx.dispose()

        val x0 = x0.toIntOr()
        val y0 = y0.toIntOr()
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