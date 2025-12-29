package me.anno.jvm.fonts

import me.anno.fonts.FontStats
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths
import me.anno.utils.Clock
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.util.Locale
import kotlin.math.ceil

object FontManagerImpl {

    private val LOGGER = LogManager.getLogger(FontManagerImpl::class)
    private val awtFonts = HashMap<me.anno.fonts.Font, FontData>()

    fun register() {
        FontStats.getTextGeneratorImpl = { AWTFont }
        FontStats.queryInstalledFontsImpl = FontManagerImpl::getInstalledFonts
        FontStats.getDefaultFontSizeImpl = FontManagerImpl::getDefaultFontSize
        runOnNonGFXThread("SubpixelLayout") { SubpixelOffsets.calculateSubpixelOffsets() }
    }

    private fun getDefaultFontSize(): Int {
        return Maths.clamp(Toolkit.getDefaultToolkit().screenSize.height / 72, 15, 60)
    }

    fun getAWTFont(font: me.anno.fonts.Font): FontData {
        val name = font.name
        return synchronized(awtFonts) {
            awtFonts.getOrPut(font) {
                val style = font.isItalic.toInt(Font.ITALIC) or
                        font.isBold.toInt(Font.BOLD)
                getDefaultFont(name)?.deriveFont(style, font.size)
                    ?: throw RuntimeException("Font $name was not found")
            }
        }
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
        val awtFont = getAWTFont(font).awtFont
        val ctx = FontRenderContext(null, true, true)
        val length = TextLayout(codepoint.joinChars(), awtFont, ctx).bounds.maxX
        return length.roundToIntOr()
    }

    fun getTextLength2(font: me.anno.fonts.Font, codepointA: Int, codepointB: Int): Int {
        val awtFont = getAWTFont(font).awtFont
        val ctx = FontRenderContext(null, true, true)
        val text = listOf(codepointA, codepointB).joinChars().toString()
        val length = TextLayout(text, awtFont, ctx).bounds.maxX
        return length.roundToIntOr()
    }

    private fun getDefaultFont(name: String): FontData? {
        val key = me.anno.fonts.Font(
            name, 12f, isBold = false, isItalic = false,
            4f, 0f,
            isEqualSpaced = "mono" in name, 1.5f
        )
        val cached = awtFonts[key]
        if (cached != null) return cached
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
        val fontI = FontData(font ?: return null)
        awtFonts[key] = fontI
        return fontI
    }

    private fun loadFont(ref: FileReference, callback: (Font?) -> Unit) {
        ref.inputStream { it, _ ->
            if (it != null) {
                it.use {
                    // what is type1_font?
                    val font = Font.createFont(Font.TRUETYPE_FONT, it)
                    GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .registerFont(font)
                    callback(font)
                }
            } else callback(null)
        }
    }
}