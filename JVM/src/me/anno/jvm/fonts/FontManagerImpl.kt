package me.anno.jvm.fonts

import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.FontImpl
import me.anno.fonts.FontManager
import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.fonts.FontManager.getFontSizeIndex
import me.anno.fonts.FontStats
import me.anno.fonts.IEmojiCache
import me.anno.fonts.keys.FontKey
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths
import me.anno.utils.Clock
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.Threads.runOnNonGFXThread
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.util.Locale

object FontManagerImpl {

    private val LOGGER = LogManager.getLogger(FontManagerImpl::class)
    private val awtFonts = HashMap<FontKey, Font>()

    fun register() {
        FontStats.getTextGeneratorImpl = FontManagerImpl::getTextGenerator
        FontStats.queryInstalledFontsImpl = FontManagerImpl::getInstalledFonts
        FontStats.getTextLengthImpl = FontManagerImpl::getTextLength
        FontStats.getDefaultFontSizeImpl = FontManagerImpl::getDefaultFontSize
        runOnNonGFXThread("SubpixelLayout") { SubpixelOffsets.calculateSubpixelOffsets() }
    }

    private fun getDefaultFontSize(): Int {
        return Maths.clamp(Toolkit.getDefaultToolkit().screenSize.height / 72, 15, 60)
    }

    private fun getTextGenerator(key: FontKey): FontImpl<*> {
        return AWTFont(key, getAWTFont(key))
    }

    fun getAWTFont(key: FontKey): Font {
        val name = key.name
        val boldItalicStyle = key.italic.toInt(Font.ITALIC) or key.bold.toInt(Font.BOLD)
        val size = getAvgFontSize(key.sizeIndex)
        return awtFonts[key] ?: getDefaultFont(name)?.deriveFont(boldItalicStyle, size)
        ?: throw RuntimeException("Font $name was not found")
    }

    fun getAWTFont(font: me.anno.fonts.Font): Font {
        val key = FontKey(font.name, getFontSizeIndex(font.size), font.isBold, font.isItalic)
        return getAWTFont(key)
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

    private fun getTextLength(font: me.anno.fonts.Font, text: String): Double {
        val awtFont = (FontManager.getFont(font) as AWTFont).awtFont
        val ctx = FontRenderContext(null, true, true)

        val codepoints = text.codepoints().asList()
        val emojiCache = IEmojiCache.emojiCache
        if (emojiCache.getEmojiId(codepoints) >= 0) {
            return font.sizeInt.toDouble() // do we need padding???
        }

        return TextLayout(text, awtFont, ctx).bounds.maxX
    }

    private fun getDefaultFont(name: String): Font? {
        val key = FontKey(name, Int.MIN_VALUE, bold = false, italic = false)
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
        awtFonts[key] = font ?: return null
        return font
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