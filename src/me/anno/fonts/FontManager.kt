package me.anno.fonts

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.cache.LRUCache
import me.anno.fonts.FontStats.getTextGenerator
import me.anno.fonts.FontStats.queryInstalledFonts
import me.anno.fonts.keys.FontKey
import me.anno.fonts.keys.TextCacheKey
import me.anno.fonts.keys.TextCacheKey.Companion.getTextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.hpc.ProcessingQueue
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

    private val fonts = HashMap<FontKey, FontImpl<*>>()

    val fontSet by lazy {
        queryInstalledFonts().toSortedSet()
    }

    fun getFontSizeIndex(fontSize: Float): Int = max(0, round(100.0 * ln(fontSize)).toInt())
    fun getAvgFontSize(fontSizeIndex: Int): Float = exp(fontSizeIndex * 0.01f)

    fun limitWidth(font: Font, widthLimit: Int): Int {
        return if (widthLimit < 0 || widthLimit >= GFX.maxTextureSize || font.size < 1f) {
            GFX.maxTextureSize
        } else {
            val step = max(1, font.size.toInt())
            ceilDiv(widthLimit, step) * step
        }
    }

    private fun roundHeightLimitToNumberOfLines(font: Font, heightLimit: Int): Int {
        val fontHeight = font.size // roughly, not exact!
        val spaceBetweenLines = spaceBetweenLines(fontHeight)
        val effLineHeight = font.sizeInt + spaceBetweenLines
        return max(ceilDiv(heightLimit, effLineHeight), 0) * effLineHeight
    }

    fun limitHeight(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        return if (heightLimit < 0 || heightLimit >= GFX.maxTextureSize) GFX.maxTextureSize
        else {
            val size = getSize(font, text, widthLimit, GFX.maxTextureSize).value
            val heightLimit2 =
                if (size != null) min(getSizeY(size), heightLimit) // doesn't really need rounding...
                else heightLimit
            roundHeightLimitToNumberOfLines(font, heightLimit2)
        }
    }

    fun getSize(key: TextCacheKey): AsyncCacheData<Int> {
        return textSizeCache.getEntry(key, textSizeTimeoutMillis, fontQueue, textSizeGenerator)
    }

    private val textSizeGenerator = { key: TextCacheKey, result: AsyncCacheData<Int> ->
        val font = getFont(key)
        val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
        val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
        result.value = font.calculateSize(key.text, wl, hl)
    }

    fun getSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): AsyncCacheData<Int> {
        if (text.isEmpty()) return font.emptySize
        return getSize(getTextCacheKey(font, text, widthLimit, heightLimit, false))
    }

    fun getBaselineY(font: Font): Float {
        return getFont(font).getBaselineY()
    }

    fun getLineHeight(font: Font): Float {
        return getFont(font).getLineHeight()
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

    fun getFont(key: TextCacheKey): FontImpl<*> = getFont(
        key.fontName, getAvgFontSize(key.fontSizeIndex()),
        key.isBold(), key.isItalic(),
        key.relativeTabSize, key.relativeCharSpacing
    )

    fun getFont(font: Font): FontImpl<*> = getFont(
        font.name, font.size,
        font.isBold, font.isItalic,
        font.relativeTabSize, font.relativeCharSpacing
    )

    fun getFont(
        name: String, fontSize: Float, bold: Boolean, italic: Boolean,
        relativeTabSize: Float, relativeCharSpacing: Float
    ): FontImpl<*> {
        val fontSizeIndex = getFontSizeIndex(fontSize)
        return getFont(
            name, fontSizeIndex, bold, italic,
            relativeTabSize, relativeCharSpacing
        )
    }

    private fun getFont(
        name: String, fontSizeIndex: Int, bold: Boolean, italic: Boolean,
        relativeTabSize: Float, relativeCharSpacing: Float
    ): FontImpl<*> {
        val key = FontKey(name, fontSizeIndex, bold, italic, relativeTabSize, relativeCharSpacing)
        return fonts.getOrPut(key) { getTextGenerator(key) }
    }

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToIntOr()
}