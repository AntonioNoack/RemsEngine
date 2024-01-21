package me.anno.fonts

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.fonts.AWTFont.Companion.spaceBetweenLines
import me.anno.fonts.keys.FontKey
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Clock
import me.anno.utils.Sleep.waitUntil
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.util.*
import kotlin.math.*

object FontManager {

    val TextCache = CacheSection("Text")
    val TextSizeCache = CacheSection("TextSize")

    private val LOGGER = LogManager.getLogger(FontManager::class)

    private const val textureTimeout = 10_000L

    private val awtFonts = HashMap<FontKey, Font>()
    private val fonts = HashMap<FontKey, AWTFont>()

    val fontList by lazy {
        val tick = Clock()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fontNames = ge.getAvailableFontFamilyNames(Locale.ROOT).toList()
        // 0.17s on Win 10, R5 2600, a few extra fonts
        // this lag would not be acceptable :)
        // worst-case-scenario: list too long, and no fonts are returned
        // (because of that, the already used one is added)
        tick.stop("getting the font list")
        fontNames
    }

    val fontSet by lazy {
        fontList.toSortedSet()
    }

    fun getFontSizeIndex(fontSize: Float): Int = max(0, round(100.0 * ln(fontSize)).toInt())
    fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun limitWidth(
        font: me.anno.ui.base.Font,
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int
    ): Int {
        return if (widthLimit < 0 || widthLimit >= GFX.maxTextureSize || font.size < 1f) GFX.maxTextureSize
        else {
            val size0 = getSize(font, text, GFX.maxTextureSize, heightLimit, false)
            val w = GFXx2D.getSizeX(size0)
            if (w <= widthLimit) return size0
            val step = max(1, font.size.toInt())
            min(w, (widthLimit + step / 2) / step * step)
        }
    }

    fun limitHeight(font: me.anno.ui.base.Font, heightLimit: Int): Int {
        val fontHeight = font.size // roughly, not exact!
        val spaceBetweenLines = spaceBetweenLines(fontHeight)
        // val height = (fontHeight + spaceBetweenLines) * lineCount - spaceBetweenLines
        // val lineCount = ceil((heightLimit - spaceBetweenLines) / (fontHeight + spaceBetweenLines)).toInt()
        val effLineHeight = font.sizeInt + spaceBetweenLines
        return ceilDiv(heightLimit, effLineHeight)
    }

    fun limitHeight(
        font: me.anno.ui.base.Font,
        text: CharSequence,
        widthLimit2: Int,
        heightLimit: Int
    ): Int {
        return if (heightLimit < 0 || heightLimit >= GFX.maxTextureSize) GFX.maxTextureSize
        else {
            val size0 = getSize(font, text, widthLimit2, GFX.maxTextureSize, false)
            val h = GFXx2D.getSizeY(size0)
            limitHeight(font, min(h, heightLimit))
        }
    }

    fun getSize(key: TextCacheKey, async: Boolean): Int {
        val data = TextSizeCache.getEntry(key, 100_000, async) {
            val awtFont = getFont(it)
            val averageFontSize = getAvgFontSize(it.fontSizeIndex())
            val wl = if (it.widthLimit < 0) GFX.maxTextureSize else min(it.widthLimit, GFX.maxTextureSize)
            val hl = if (it.heightLimit < 0) GFX.maxTextureSize else min(it.heightLimit, GFX.maxTextureSize)
            CacheData(awtFont.calculateSize(it.text, averageFontSize, wl, hl))
        } as? CacheData<*>
        return data?.value as? Int ?: -1
    }

    fun getSize(
        font: me.anno.ui.base.Font,
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        async: Boolean
    ): Int {
        if (text.isEmpty()) return GFXx2D.getSize(0, font.sizeInt)
        return getSize(TextCacheKey.getKey(font, text, widthLimit, heightLimit, false), async)
    }

    fun getBaselineY(
        font: me.anno.ui.base.Font
    ): Float {
        // val f = getFont(font)
        // why ever...
        // Consolas example:
        // font size 40
        // best value: 17/18
        // ascent: 30
        // descent: 10
        return -font.size * 17f / 40f
    }

    fun getTextCacheKey(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int,
        async: Boolean
    ): TextCacheKey? {

        if (text.isBlank2()) return null
        val fontSize = font.size

        val fontSizeIndex = getFontSizeIndex(fontSize)
        val sub = fontSizeIndex * 8 + font.isItalic.toInt(4) + font.isBold.toInt(2)

        val wl = if (widthLimit < 0) GFX.maxTextureSize else min(widthLimit, GFX.maxTextureSize)
        val hl = if (heightLimit < 0) GFX.maxTextureSize else min(heightLimit, GFX.maxTextureSize)

        val wl2 = limitWidth(font, text, wl, hl)
        val hl2 = limitHeight(font, text, wl2, hl)

        val fontName = font.name
        return TextCacheKey(text, fontName, sub, wl2, hl2)
    }

    fun getTexture(
        font: me.anno.ui.base.Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int,
        async: Boolean
    ): ITexture2D? {
        val wl = if (widthLimit < 0) GFX.maxTextureSize else min(widthLimit, GFX.maxTextureSize)
        val hl = if (heightLimit < 0) GFX.maxTextureSize else min(heightLimit, GFX.maxTextureSize)
        val key = getTextCacheKey(font, text, wl, hl, false) ?: return null
        return getTexture(key, async)
    }

    fun getASCIITexture(font: me.anno.ui.base.Font): Texture2DArray {
        return TextCache.getEntry(font, textureTimeout, false) { key ->
            getFont(key).generateASCIITexture(false)
        } as Texture2DArray
    }

    fun getTexture(cacheKey: TextCacheKey, async: Boolean): ITexture2D? {
        // must be sync:
        // - textures need to be available
        // - Java/Windows is not thread-safe
        if (cacheKey.text.isBlank2()) return null
        return TextCache.getEntry(cacheKey, textureTimeout, async) { key ->
            val font2 = getFont(key)
            val averageFontSize = getAvgFontSize(key.fontSizeIndex())
            val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
            val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
            val texture = font2.generateTexture(key.text, averageFontSize, wl, hl, key.isGrayscale())
            if (texture == null) LOGGER.warn("Texture for '$key' was null")
            texture
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
            me.anno.ui.base.Font(name, fontSize, bold, italic),
            awtFonts[key] ?: getDefaultFont(name)?.deriveFont(boldItalicStyle, fontSize)
            ?: throw RuntimeException("Font $name was not found")
        )
        fonts[key] = font2
        return font2
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