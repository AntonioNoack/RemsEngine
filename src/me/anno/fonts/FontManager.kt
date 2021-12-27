package me.anno.fonts

import me.anno.cache.CacheData
import me.anno.cache.instances.TextCache
import me.anno.cache.instances.TextSizeCache
import me.anno.fonts.keys.FontKey
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.Clock
import me.anno.utils.maths.Maths.ceilDiv
import me.anno.utils.hpc.Threads.threadWithName
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import kotlin.math.*

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
            hasFonts = true
            threadWithName("FontManager") {
                synchronized(awtFontList) {
                    val tick = Clock()
                    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    val fontNames = ge.availableFontFamilyNames
                    awtFontList.clear()
                    awtFontList += fontNames
                    // 0.17s on Win 10, R5 2600, a few extra fonts
                    // this lag would not be acceptable :)
                    // worst-case-scenario: list too long, and no fonts are returned
                    // (because of that, the already used one is added)
                    tick.stop("getting the font list")
                    callback(awtFontList)
                }
            }
        }
    }

    fun getFontSizeIndex(fontSize: Float): Int = max(0, round(100.0 * ln(fontSize)).toInt())
    fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun limitWidth(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int
    ): Int {
        return if (widthLimit < 0) -1 else {
            loadTexturesSync.push(true)
            val size0 = getSize(font, text, -1, heightLimit)
            val w = GFXx2D.getSizeX(size0)
            if (w <= widthLimit) return size0
            loadTexturesSync.pop()
            val step = font.size.toInt()
            min(w, (widthLimit + step / 2) / step * step)
        }
    }

    fun limitHeight(font: me.anno.ui.base.Font, heightLimit: Int): Int {
        val fontHeight = font.size // roughly, not exact!
        val spaceBetweenLines = getFont(font).spaceBetweenLines(fontHeight)
        // val height = (fontHeight + spaceBetweenLines) * lineCount - spaceBetweenLines
        // val lineCount = ceil((heightLimit - spaceBetweenLines) / (fontHeight + spaceBetweenLines)).toInt()
        val effLineHeight = font.sizeInt + spaceBetweenLines
        return ceilDiv(heightLimit, effLineHeight)
    }

    fun limitHeight(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit2: Int,
        heightLimit: Int
    ): Int {
        return if (heightLimit < 0) -1 else {
            loadTexturesSync.push(true)
            val size0 = getSize(font, text, widthLimit2, -1)
            val h = GFXx2D.getSizeY(size0)
            loadTexturesSync.pop()
            limitHeight(font, min(h, heightLimit))
        }
    }

    fun getSize(
        key: TextCacheKey
    ): Int {
        GFX.checkIsGFXThread()
        val data = TextSizeCache.getEntry(key, 100_000, false) {
            val awtFont = getFont(it)
            val averageFontSize = getAvgFontSize(it.fontSizeIndex())
            CacheData(awtFont.calculateSize(it.text, averageFontSize, it.widthLimit, it.heightLimit))
        } as CacheData<*>
        return data.value as Int
    }

    fun getSize(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int
    ): Int {

        if (text.isEmpty()) return GFXx2D.getSize(0, font.sizeInt)

        val fontSizeIndex = font.sizeIndex
        val properties = TextCacheKey.getProperties(fontSizeIndex, font)

        val widthLimit2 = limitWidth(font, text, widthLimit, heightLimit)
        val lineCountLimit = limitHeight(font, text, widthLimit2, heightLimit)

        val fontName = font.name
        val key = TextCacheKey(text, fontName, properties, widthLimit2, lineCountLimit)
        return getSize(key)

    }

    fun getTextCacheKey(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int
    ): TextCacheKey? {

        if (text.isBlank2()) return null
        val fontSize = font.size
        val fontSizeIndex = getFontSizeIndex(fontSize)
        val sub = fontSizeIndex * 8 + font.isItalic.toInt(4) + font.isBold.toInt(2)
        val widthLimit2 = limitWidth(font, text, widthLimit, heightLimit)
        val lineCountLimit = limitHeight(font, text, widthLimit2, heightLimit)

        val fontName = font.name
        return TextCacheKey(text, fontName, sub, widthLimit2, lineCountLimit)

    }

    fun getString(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int
    ): ITexture2D? {
        val key = getTextCacheKey(font, text, widthLimit, heightLimit) ?: return null
        return getString(key)
    }

    fun getString(
        cacheKey: TextCacheKey
    ): ITexture2D? {
        // must be sync:
        // - textures need to be available
        // - Java/Windows is not thread-safe
        return TextCache.getEntry(cacheKey, textureTimeout, false) { key ->
            val font2 = getFont(key)
            val averageFontSize = getAvgFontSize(key.fontSizeIndex())
            val texture = font2.generateTexture(key.text, averageFontSize, key.widthLimit, key.heightLimit, false)
            if (texture == null) LOGGER.warn("Texture for '$key' was null")
            texture ?: TextureLib.nullTexture
        } as? ITexture2D
    }

    fun getFont(key: TextCacheKey): AWTFont =
        getFont(key.fontName, getAvgFontSize(key.fontSizeIndex()), key.isBold(), key.isItalic())

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