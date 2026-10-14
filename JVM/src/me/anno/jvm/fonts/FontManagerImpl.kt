package me.anno.jvm.fonts

import me.anno.fonts.FontStats
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.jvm.fonts.AWTFont.getScale
import me.anno.maths.Maths
import me.anno.utils.Clock
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.font.TextLayout
import java.util.Locale
import kotlin.math.sqrt

object FontManagerImpl {

    const val DEFAULT_FONT_SIZE = 12f

    private val LOGGER = LogManager.getLogger(FontManagerImpl::class)

    private val awtFonts = HashMap<FontKey, FontData>()

    fun register() {
        FontStats.getTextGeneratorImpl = { AWTFont }
        FontStats.queryInstalledFontsImpl = FontManagerImpl::getInstalledFonts
        FontStats.getDefaultFontSizeImpl = FontManagerImpl::getDefaultFontSize
        runOnNonGFXThread("SubpixelLayout") { SubpixelOffsets.calculateSubpixelOffsets() }
    }

    private fun getDefaultFontSize(): Int {
        val size0 = Toolkit.getDefaultToolkit().screenSize.height / 1080f
        return Maths.clamp((sqrt(size0) * 15f).toInt(), 15, 60)
    }

    fun getAWTFont(font: me.anno.fonts.Font): FontData {
        val name = font.name
        val result = synchronized(awtFonts) {
            val style = font.isItalic.toInt(Font.ITALIC) or
                    font.isBold.toInt(Font.BOLD)
            val key = FontKey(name, style)
            awtFonts.getOrPut(key) {
                val font0 = getDefaultFont(name) ?: error("Font $name was not found")
                if (style == 0) font0 else font0.deriveFont(style, DEFAULT_FONT_SIZE)
            }
        }
        assertEquals(font.isBold, result.awtFont.isBold)
        assertEquals(font.isItalic, result.awtFont.isItalic)
        return result
    }

    private fun getInstalledFonts(): List<String> {
        val tick = Clock(LOGGER)
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fontNames = ge.getAvailableFontFamilyNames(Locale.ROOT).toList()
        // 0.17s on Win 10, R5 2600, a few extra fonts
        // this lag would not be acceptable :)
        // worst-case-scenario: list too long, and no fonts are returned
        // (because of that, the already used one is added)
        tick.stop("getting the font list")
        return fontNames
    }

    fun getTextLength1(font: me.anno.fonts.Font, codepoint: Int): Int {
        val awtFont = getAWTFont(font)
        val layout = TextLayout(codepoint.joinChars(), awtFont.awtFont, awtFont.renderContext)
        val length = layout.bounds.maxX * getScale(font)
        return length.roundToIntOr()
    }

    fun getTextLength2(font: me.anno.fonts.Font, codepointA: Int, codepointB: Int): Int {
        val awtFont = getAWTFont(font)
        val text = listOf(codepointA, codepointB).joinChars().toString()
        val layout = TextLayout(text, awtFont.awtFont, awtFont.renderContext)
        val length = layout.bounds.maxX * getScale(font)
        return length.roundToIntOr()
    }

    private fun getDefaultFont(name: String): FontData? {
        val font = if ('/' in name) {
            var font: Font? = null
            var hasFont = false
            loadFont(getReference(name)) {
                font = it
                hasFont = true
            }
            waitUntil(true) { hasFont }
            font
        } else Font.decode(name)
        return FontData(font ?: return null)
    }

    private fun loadFont(ref: FileReference, callback: (Font?) -> Unit) {
        ref.inputStream { stream0, _ ->
            if (stream0 != null) {
                stream0.use { stream ->
                    // what is type1_font?
                    val font = Font.createFont(Font.TRUETYPE_FONT, stream)
                    GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .registerFont(font)
                    callback(font)
                }
            } else callback(null)
        }
    }
}