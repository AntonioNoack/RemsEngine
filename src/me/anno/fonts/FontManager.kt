package me.anno.fonts

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.LRUCache
import me.anno.fonts.FontStats.getTextGenerator
import me.anno.fonts.FontStats.queryInstalledFonts
import me.anno.fonts.keys.FontKey
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

object FontManager {

    val TextCache = CacheSection("Text")
    val TextSizeCache = CacheSection("TextSize")

    private val LOGGER = LogManager.getLogger(FontManager::class)

    private const val textureTimeout = 10_000L

    private val fonts = HashMap<FontKey, TextGenerator>()

    val fontSet by lazy {
        queryInstalledFonts().toSortedSet()
    }

    fun getFontSizeIndex(fontSize: Float): Int = max(0, round(100.0 * ln(fontSize)).toInt())
    fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun limitWidth(
        font: Font,
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

    fun limitHeight(font: Font, heightLimit: Int): Int {
        val fontHeight = font.size // roughly, not exact!
        val spaceBetweenLines = spaceBetweenLines(fontHeight)
        // val height = (fontHeight + spaceBetweenLines) * lineCount - spaceBetweenLines
        // val lineCount = ceil((heightLimit - spaceBetweenLines) / (fontHeight + spaceBetweenLines)).toInt()
        val effLineHeight = font.sizeInt + spaceBetweenLines
        return ceilDiv(heightLimit, effLineHeight)
    }

    fun limitHeight(
        font: Font,
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
            val wl = if (it.widthLimit < 0) GFX.maxTextureSize else min(it.widthLimit, GFX.maxTextureSize)
            val hl = if (it.heightLimit < 0) GFX.maxTextureSize else min(it.heightLimit, GFX.maxTextureSize)
            CacheData(awtFont.calculateSize(it.text, wl, hl))
        } as? CacheData<*>
        return data?.value as? Int ?: -1
    }

    fun getSize(
        font: Font,
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        async: Boolean
    ): Int {
        if (text.isEmpty()) return GFXx2D.getSize(0, font.sizeInt)
        return getSize(TextCacheKey.getKey(font, text, widthLimit, heightLimit, false), async)
    }

    fun getBaselineY(
        font: Font
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
        font: Font,
        text: String,
        widthLimit: Int,
        heightLimit: Int
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
        font: Font, text: String,
        widthLimit: Int, heightLimit: Int, async: Boolean
    ): ITexture2D? {
        return getTexture(font, text, widthLimit, heightLimit, textureTimeout, async)
    }

    fun getTexture(
        font: Font, text: String,
        widthLimit: Int, heightLimit: Int,
        timeoutMillis: Long, async: Boolean
    ): ITexture2D? {
        val wl = if (widthLimit < 0) GFX.maxTextureSize else min(widthLimit, GFX.maxTextureSize)
        val hl = if (heightLimit < 0) GFX.maxTextureSize else min(heightLimit, GFX.maxTextureSize)
        val key = getTextCacheKey(font, text, wl, hl) ?: return null
        return getTexture(key, timeoutMillis, async)
    }

    private val asciiTexLRU = LRUCache<Font, Texture2DArray>(16)
    fun getASCIITexture(font: Font): Texture2DArray {
        val prev = asciiTexLRU[font]
        if (prev is Texture2DArray && prev.isCreated()) {
            return prev
        }
        val entry = TextCache.getEntry(font, textureTimeout, false) { key ->
            val entry = AsyncCacheData<Texture2DArray>()
            getFont(key).generateASCIITexture(false, entry)
            entry
        } as AsyncCacheData<*>
        entry.waitForGFX()
        val curr = entry.value as Texture2DArray
        asciiTexLRU[font] = curr
        return curr
    }

    fun getTexture(cacheKey: TextCacheKey, async: Boolean): ITexture2D? {
        return getTexture(cacheKey, textureTimeout, async)
    }

    fun getTexture(cacheKey: TextCacheKey, timeoutMillis: Long, async: Boolean): ITexture2D? {
        // must be sync:
        // - textures need to be available
        // - Java/Windows is not thread-safe
        if (cacheKey.text.isBlank2()) return null
        val entry = TextCache.getEntry(cacheKey, timeoutMillis, async) { key ->
            val font2 = getFont(key)
            val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
            val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
            val entry = AsyncCacheData<ITexture2D>()
            font2.generateTexture(key.text, wl, hl, key.isGrayscale(), entry)
            entry
        } as? AsyncCacheData<*>
        if (!async) entry?.waitForGFX()
        return entry?.value as? ITexture2D
    }

    fun getFont(key: TextCacheKey): TextGenerator =
        getFont(key.fontName, getAvgFontSize(key.fontSizeIndex()), key.isBold(), key.isItalic())

    fun getFont(font: Font): TextGenerator =
        getFont(font.name, font.size, font.isBold, font.isItalic)

    fun getFont(name: String, fontSize: Float, bold: Boolean, italic: Boolean): TextGenerator {
        val fontSizeIndex = getFontSizeIndex(fontSize)
        return getFont(name, fontSizeIndex, bold, italic)
    }

    private fun getFont(
        name: String,
        fontSizeIndex: Int,
        bold: Boolean,
        italic: Boolean
    ): TextGenerator {
        val key = FontKey(name, fontSizeIndex, bold, italic)
        return fonts.getOrPut(key) {
            getTextGenerator(key)
        }
    }

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToInt()
}