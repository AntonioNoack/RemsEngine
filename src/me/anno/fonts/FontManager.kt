package me.anno.fonts

import me.anno.cache.AsyncCacheData
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
import me.anno.utils.assertions.assertEquals
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object FontManager {

    val textAtlasCache = CacheSection<Font, Texture2DArray>("TextAtlas")
    val textTextureCache = CacheSection<TextCacheKey, ITexture2D>("TextTexture")
    val textSizeCache = CacheSection<TextCacheKey, Int>("TextSize")

    private val fontQueue = ProcessingQueue("FontManager")

    private const val textureTimeoutMillis = 10_000L
    private const val textSizeTimeoutMillis = 100_000L

    private val fonts = HashMap<FontKey, TextGenerator>()

    val fontSet by lazy {
        queryInstalledFonts().toSortedSet()
    }

    fun getFontSizeIndex(fontSize: Float): Int = max(0, round(100.0 * ln(fontSize)).toInt())
    fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun limitWidth(
        font: Font, text: CharSequence,
        widthLimit: Int, heightLimit: Int
    ): Int {
        return if (widthLimit < 0 || widthLimit >= GFX.maxTextureSize || font.size < 1f) GFX.maxTextureSize
        else {
            val size0 = getSize(font, text, GFX.maxTextureSize, heightLimit).waitFor() ?: 0
            val w = GFXx2D.getSizeX(size0)
            if (w <= widthLimit) return size0
            val step = max(1, font.size.toInt())
            min(w, (widthLimit + step / 2) / step * step)
        }
    }

    fun limitHeight(font: Font, heightLimit: Int): Int {
        val fontHeight = font.size // roughly, not exact!
        val spaceBetweenLines = spaceBetweenLines(fontHeight)
        val effLineHeight = font.sizeInt + spaceBetweenLines
        return max(ceilDiv(heightLimit, effLineHeight), 0) * effLineHeight
    }

    fun limitHeight(font: Font, text: CharSequence, widthLimit2: Int, heightLimit: Int): Int {
        return if (heightLimit < 0 || heightLimit >= GFX.maxTextureSize) GFX.maxTextureSize
        else {
            val size0 = getSize(font, text, widthLimit2, GFX.maxTextureSize)
                .waitFor() ?: font.sizeInt
            val h = GFXx2D.getSizeY(size0)
            limitHeight(font, min(h, heightLimit))
        }
    }

    fun getSize(key: TextCacheKey): AsyncCacheData<Int> {
        return textSizeCache.getEntry(key, textSizeTimeoutMillis, fontQueue, textSizeGenerator)
    }

    private val textSizeGenerator = { keyI: TextCacheKey, result: AsyncCacheData<Int> ->
        val awtFont = getFont(keyI)
        val wl = if (keyI.widthLimit < 0) GFX.maxTextureSize else min(keyI.widthLimit, GFX.maxTextureSize)
        val hl = if (keyI.heightLimit < 0) GFX.maxTextureSize else min(keyI.heightLimit, GFX.maxTextureSize)
        result.value = awtFont.calculateSize(keyI.text, wl, hl)
    }

    fun getSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): AsyncCacheData<Int> {
        if (text.isEmpty()) return font.emptySize
        val key = TextCacheKey.getKey(font, text, widthLimit, heightLimit, false)
        val key1 = TextCacheKey.getKey(font, text, widthLimit, heightLimit, false)
        assertEquals(key, key1)
        return getSize(key)
    }

    fun getBaselineY(font: Font): Float {
        return getFont(font).getBaselineY()
    }

    fun getLineHeight(font: Font): Float {
        return getFont(font).getLineHeight()
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

    fun getTexture(font: Font, text: String, widthLimit: Int, heightLimit: Int): AsyncCacheData<ITexture2D> {
        return getTexture(font, text, widthLimit, heightLimit, textureTimeoutMillis)
    }

    fun getTexture(
        font: Font, text: String,
        widthLimit: Int, heightLimit: Int,
        timeoutMillis: Long
    ): AsyncCacheData<ITexture2D> {
        val wl = if (widthLimit < 0) GFX.maxTextureSize else min(widthLimit, GFX.maxTextureSize)
        val hl = if (heightLimit < 0) GFX.maxTextureSize else min(heightLimit, GFX.maxTextureSize)
        val key = getTextCacheKey(font, text, wl, hl) ?: return AsyncCacheData.empty()
        return getTexture(key, timeoutMillis)
    }

    private val asciiTexLRU = LRUCache<Font, Texture2DArray>(16)
    fun getASCIITexture(font: Font): Texture2DArray {
        val prev = asciiTexLRU[font]
        if (prev is Texture2DArray) {
            prev.checkSession()
            if (prev.isCreated()) {
                return prev
            }
        }
        val curr = textAtlasCache.getEntry(font, textureTimeoutMillis, generateAtlas)
            .waitFor() as Texture2DArray
        // todo it would be nice if we could prioritize loading our task
        asciiTexLRU[font] = curr
        return curr
    }

    private val generateAtlas = { key: Font, result: AsyncCacheData<Texture2DArray> ->
        getFont(key).generateASCIITexture(false, result)
    }

    fun getTexture(cacheKey: TextCacheKey): AsyncCacheData<ITexture2D> {
        return getTexture(cacheKey, textureTimeoutMillis)
    }

    fun getTexture(cacheKey: TextCacheKey, timeoutMillis: Long): AsyncCacheData<ITexture2D> {
        // must be sync:
        // - textures need to be available
        // - Java/Windows is not thread-safe
        if (cacheKey.text.isBlank2()) return AsyncCacheData.empty()
        return textTextureCache.getEntry(cacheKey, timeoutMillis, fontQueue, generateTexture)
    }

    private val generateTexture = { key: TextCacheKey, result: AsyncCacheData<ITexture2D> ->
        val font2 = getFont(key)
        val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
        val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
        font2.generateTexture(key.text, wl, hl, key.isGrayscale(), result)
    }

    fun getFont(key: TextCacheKey): TextGenerator =
        getFont(key.fontName, getAvgFontSize(key.fontSizeIndex()), key.isBold(), key.isItalic())

    fun getFont(font: Font): TextGenerator =
        getFont(font.name, font.size, font.isBold, font.isItalic)

    fun getFont(name: String, fontSize: Float, bold: Boolean, italic: Boolean): TextGenerator {
        val fontSizeIndex = getFontSizeIndex(fontSize)
        return getFont(name, fontSizeIndex, bold, italic)
    }

    private fun getFont(name: String, fontSizeIndex: Int, bold: Boolean, italic: Boolean): TextGenerator {
        val key = FontKey(name, fontSizeIndex, bold, italic)
        return fonts.getOrPut(key) {
            getTextGenerator(key)
        }
    }

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToIntOr()
}