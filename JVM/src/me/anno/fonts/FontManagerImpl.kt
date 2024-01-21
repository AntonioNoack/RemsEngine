package me.anno.fonts

import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.fonts.keys.FontKey
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.utils.Clock
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.types.Booleans.toInt
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.*

object FontManagerImpl {

    private val awtFonts = HashMap<FontKey, Font>()

    fun getTextGenerator(key: FontKey): TextGenerator {
        val name = key.name
        val boldItalicStyle = key.italic.toInt(Font.ITALIC) or key.bold.toInt(Font.BOLD)
        val size = getAvgFontSize(key.sizeIndex)
        return AWTFont(
            Font(name, size, key.bold, key.italic),
            awtFonts[key] ?: getDefaultFont(name)?.deriveFont(boldItalicStyle, size)
            ?: throw RuntimeException("Font $name was not found")
        )
    }

    fun getInstalledFonts(): List<String> {
        val tick = Clock()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fontNames = ge.getAvailableFontFamilyNames(Locale.ROOT).toList()
        // 0.17s on Win 10, R5 2600, a few extra fonts
        // this lag would not be acceptable :)
        // worst-case-scenario: list too long, and no fonts are returned
        // (because of that, the already used one is added)
        tick.stop("getting the font list")
        return fontNames
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