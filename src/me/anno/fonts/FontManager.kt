package me.anno.fonts

import me.anno.cache.CacheData
import me.anno.cache.instances.TextCache
import me.anno.fonts.keys.FontKey
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.TextureLib
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import kotlin.concurrent.thread
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.round

object FontManager {

    private val LOGGER = LogManager.getLogger(FontManager::class)!!

    private const val textureTimeout = 10000L

    private var hasFonts = false
    private val awtFontList = ArrayList<String>()
    private val awtFonts = HashMap<FontKey, Font>()

    private val fonts = HashMap<FontKey, AWTFont>()

    fun requestFontList(callback: (List<String>) -> Unit) {
        if (hasFonts) callback(awtFontList)
        else {
            thread {
                synchronized(awtFontList) {
                    hasFonts = true
                    val t0 = System.nanoTime()
                    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    val fontNames = ge.availableFontFamilyNames
                    awtFontList.clear()
                    awtFontList += fontNames
                    val t1 = System.nanoTime()
                    // 0.17s on Win 10, R5 2600, a few extra fonts
                    // this lag would not be acceptable :)
                    // worst-case-scenario: list too long, and no fonts are returned
                    // (because of that, the already used one is added)
                    LOGGER.info("Used ${((t1 - t0) * 1e-9f).f3()} to get font list")
                    callback(awtFontList)
                }
            }
        }
    }

    private fun getFontSizeIndex(fontSize: Float): Int = round(100.0 * ln(fontSize)).toInt()
    private fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun getSize(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int
    ): Pair<Int, Int> {

        val fontSize = font.size
        if (text.isEmpty()) return 0 to fontSize.toInt()
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val sub = fontSizeIndex * 8 + font.isItalic.toInt(4) + font.isBold.toInt(2) + 1
        val widthLimit2 = if (widthLimit < 0) -1 else {
            loadTexturesSync.push(true)
            val w = getSize(font, text, -1).first
            loadTexturesSync.pop()
            val step = fontSize.toInt()
            min(w, widthLimit / step * step)
        }

        val fontName = font.name
        val key = TextCacheKey(text, fontName, sub, widthLimit2)
        val data = TextCache.getEntry(key, 1000, false) {
            val font2 = getFont(font)
            val averageFontSize = getAvgFontSize(fontSizeIndex)
            CacheData(font2.calculateSize(text, averageFontSize, widthLimit2))
        } as CacheData<Pair<Int, Int>>
        return data.value

    }

    fun getString(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int
    ): ITexture2D? {

        if (text.isEmpty()) return null
        val fontSize = font.size
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val sub = fontSizeIndex * 8 + font.isItalic.toInt(4) + font.isBold.toInt(2)
        val widthLimit2 = if (widthLimit < 0) -1 else {
            loadTexturesSync.push(true)
            val w = getSize(font, text, -1).first
            loadTexturesSync.pop()
            val step = fontSize.toInt()
            min(w, widthLimit / step * step)
        }

        val fontName = font.name
        val key = TextCacheKey(text, fontName, sub, widthLimit2)
        val async = false//!loadTexturesSync.peek()
        return TextCache.getEntry(key, textureTimeout, async) {// must be sync
            val font2 = getFont(font)
            val averageFontSize = getAvgFontSize(fontSizeIndex)
            val texture = font2.generateTexture(text, averageFontSize, widthLimit2)
            texture ?: TextureLib.nullTexture
        } as? ITexture2D

    }

    fun getFont(font: me.anno.ui.base.Font): AWTFont = getFont(font.name, font.size, font.isBold, font.isItalic)

    fun getFont(name: String, fontSize: Float, bold: Boolean, italic: Boolean): AWTFont {
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val averageFontSize = getAvgFontSize(fontSizeIndex)
        return getFont(name, averageFontSize, fontSizeIndex, bold, italic)
    }

    private fun getFont(name: String, fontSize: Float, fontSizeIndex: Int, bold: Boolean, italic: Boolean): AWTFont {
        val key = FontKey(name, fontSizeIndex, bold, italic)
        val font = fonts[key]
        if (font != null) return font
        val boldItalicStyle = (if (italic) Font.ITALIC else 0) or (if (bold) Font.BOLD else 0)
        val font2 = AWTFont(
            awtFonts[key] ?: getDefaultFont(name)?.deriveFont(boldItalicStyle, fontSize)
            ?: throw RuntimeException("Font $name was not found")
        )
        fonts[key] = font2
        return font2
    }

    private fun getDefaultFont(name: String): Font? {
        val key = FontKey(name, Int.MIN_VALUE, false, false)
        val cached = awtFonts[key]
        if (cached != null) return cached
        val font = Font.decode(name) ?: return null
        awtFonts[key] = font
        return font
    }

}