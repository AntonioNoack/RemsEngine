package me.anno.fonts

import me.anno.cache.Promise
import me.anno.cache.CacheSection
import me.anno.cache.LRUCache
import me.anno.fonts.FontStats.queryInstalledFonts
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
        val effLineHeight = font.lineHeightI
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

    fun getSize(key: TextCacheKey): Promise<Int> {
        return textSizeCache.getEntry(key, textSizeTimeoutMillis, fontQueue, textSizeGenerator)
    }

    private val textSizeGenerator = { key: TextCacheKey, result: Promise<Int> ->
        val font = getFontImpl()
        val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
        val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
        result.value = font.calculateSize(key.createFont(), key.text, wl, hl)
    }

    fun getSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Promise<Int> {
        return getSize(getTextCacheKey(font, text, widthLimit, heightLimit, false))
    }

    fun getBaselineY(font: Font): Float {
        return getFontImpl().getBaselineY(font)
    }

    fun getLineHeight(font: Font): Float {
        return getFontImpl().getLineHeight(font)
    }

    fun getTexture(font: Font, text: String, widthLimit: Int, heightLimit: Int): Promise<ITexture2D> {
        return getTexture(font, text, widthLimit, heightLimit, textureTimeoutMillis)
    }

    fun getTexture(
        font: Font, text: String,
        widthLimit: Int, heightLimit: Int,
        timeoutMillis: Long
    ): Promise<ITexture2D> {
        val wl = if (widthLimit < 0) GFX.maxTextureSize else min(widthLimit, GFX.maxTextureSize)
        val hl = if (heightLimit < 0) GFX.maxTextureSize else min(heightLimit, GFX.maxTextureSize)
        val key = getTextCacheKey(font, text, wl, hl) ?: return Promise.empty()
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

    private val generateAtlas = { key: Font, result: Promise<Texture2DArray> ->
        getFontImpl().generateASCIITexture(key, false, result)
    }

    fun getTexture(cacheKey: TextCacheKey): Promise<ITexture2D> {
        return getTexture(cacheKey, textureTimeoutMillis)
    }

    fun getTexture(cacheKey: TextCacheKey, timeoutMillis: Long): Promise<ITexture2D> {
        // must be sync:
        // - textures need to be available
        // - Java/Windows/Linux is not thread-safe -> probably Java's fault
        // todo so are we calling AWTFont from always the same thread? if so, why do we still get errors???
        if (cacheKey.text.isBlank2()) return Promise.empty()
        return textTextureCache.getEntry(cacheKey, timeoutMillis, fontQueue, generateTexture)
    }

    private val generateTexture = { key: TextCacheKey, result: Promise<ITexture2D> ->
        val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
        val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
        getFontImpl().generateTexture(key.createFont(), key.text, wl, hl, key.isGrayscale(), result)
    }

    fun getFontImpl(): FontImpl<*> = FontStats.getFontImpl()

    fun spaceBetweenLines(fontSize: Float) = (0.5f * fontSize).roundToIntOr()
}