package me.anno.fonts

import me.anno.cache.CacheSection
import me.anno.cache.LRUCache
import me.anno.cache.Promise
import me.anno.fonts.FontStats.queryInstalledFonts
import me.anno.fonts.keys.CharCacheKey
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object FontManager {

    val textAtlasCache = CacheSection<Font, Texture2DArray>("TextAtlas")
    val textTextureCache = CacheSection<TextCacheKey, ITexture2D>("TextTexture")
    val charTextureCache = CacheSection<CharCacheKey, ITexture2D>("TextTexture")
    val textSizeCache = CacheSection<TextCacheKey, Int>("TextSize")

    private val fontQueue = ProcessingQueue("FontManager")

    private const val textureTimeoutMillis = 10_000L
    private const val textSizeTimeoutMillis = 100_000L

    val fontSet by lazy {
        queryInstalledFonts().toSortedSet()
    }

    fun getFontSizeIndex(fontSize: Float): Int = max(0, round(100.0 * ln(fontSize)).toInt())

    fun getSize(key: TextCacheKey): Promise<Int> {
        return textSizeCache.getEntry(key, textSizeTimeoutMillis, fontQueue, textSizeGenerator)
    }

    private val textSizeGenerator = { key: TextCacheKey, result: Promise<Int> ->
        val font = getFontImpl()
        val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
        val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
        result.value = font.calculateSize(key.font, key.text, wl, hl)
    }

    fun getSize(font: Font, text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        return getSize(TextCacheKey(font, text, widthLimit, heightLimit, false)).waitFor() ?: 0
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
        val key = TextCacheKey(font, text, wl, hl, false)
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

    fun getTexture(cacheKey: TextCacheKey, timeoutMillis: Long = textureTimeoutMillis): Promise<ITexture2D> {
        // must be sync:
        // - textures need to be available
        // - Java/Windows/Linux is not thread-safe -> probably Java's fault
        // todo so are we calling AWTFont from always the same thread? if so, why do we still get errors???
        if (cacheKey.text.isBlank2()) return Promise.empty()
        return textTextureCache.getEntry(cacheKey, timeoutMillis, fontQueue, generateTexture)
    }

    fun getTexture(cacheKey: CharCacheKey, timeoutMillis: Long = textureTimeoutMillis): Promise<ITexture2D> {
        // must be sync:
        // - textures need to be available
        // - Java/Windows/Linux is not thread-safe -> probably Java's fault
        // todo so are we calling AWTFont from always the same thread? if so, why do we still get errors???
        return charTextureCache.getEntry(cacheKey, timeoutMillis, fontQueue, generateTexture2)
    }

    private val generateTexture = { key: TextCacheKey, result: Promise<ITexture2D> ->
        val wl = if (key.widthLimit < 0) GFX.maxTextureSize else min(key.widthLimit, GFX.maxTextureSize)
        val hl = if (key.heightLimit < 0) GFX.maxTextureSize else min(key.heightLimit, GFX.maxTextureSize)
        getFontImpl().generateTexture(
            key.font, key.text,
            wl, hl, key.grayscale, result
        )
    }

    private val generateTexture2 = { key: CharCacheKey, result: Promise<ITexture2D> ->
        val wl = GFX.maxTextureSize
        val hl = GFX.maxTextureSize
        getFontImpl().generateTexture(
            key.font, key.codepoint.joinChars(),
            wl, hl, key.grayscale, result
        )
    }

    fun getFontImpl(): FontImpl<*> = FontStats.getFontImpl()
}