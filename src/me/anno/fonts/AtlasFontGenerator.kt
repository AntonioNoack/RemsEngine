package me.anno.fonts

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Codepoints.countCodepoints
import me.anno.fonts.keys.FontKey
import me.anno.gpu.drawing.DrawTexts.simpleChars
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.clamp
import me.anno.utils.OS.res
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.wait
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.min

/**
 * generates a fallback font when other text sources are unavailable using a pre-generated texture;
 * a little blurry, but much better readable than the generator, which only uses lines
 * */
class AtlasFontGenerator(val key: FontKey) : TextGenerator {

    companion object {
        private const val NUM_TILES_X = 16
        private const val NUM_TILES_Y = 6
        private val cache = CacheSection("FallbackFontGenerator")
    }

    private val fontSize = FontManager.getAvgFontSize(key.sizeIndex)
    private val charSizeY = fontSize.roundToIntOr()
    private val charSizeX = charSizeY * 7 / 12
    private val baselineY = charSizeY * 0.73f // measured in Gimp

    private fun getWrittenLength(text: CharSequence, widthLimit: Int): Int {
        return min(text.countCodepoints(), widthLimit / charSizeX)
    }

    override fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        val width = getWrittenLength(text, widthLimit) * charSizeX
        val height = charSizeY
        return GFXx2D.getSize(width, height)
    }

    private fun getImageStack(callback: (Callback<List<IntImage>>)) {
        cache.getEntryAsync(key.sizeIndex, 10_000, false, {
            val result = AsyncCacheData<List<IntImage>>()
            val source = res.getChild("textures/ASCIIAtlas.png")
            ImageCache.getAsync(source, 50, false, result.map { image ->
                image.split(NUM_TILES_X, NUM_TILES_Y).map { tileImage ->
                    tileImage
                        .resized(charSizeX, charSizeY, true)
                        .asIntImage()
                }
            })
            result
        }, callback.wait())
    }

    private fun getIndex(codepoint: Int): Int {
        return codepoint - 32
    }

    private fun generateTexture(codepoint: Int, stack: List<IntImage>): IntImage {
        return stack[clamp(getIndex(codepoint), 0, stack.lastIndex)]
    }

    private fun generateTexture(char: Char, stack: List<IntImage>): IntImage {
        return generateTexture(char.code, stack)
    }

    override fun getBaselineY(): Float {
        return baselineY
    }

    override fun getLineHeight(): Float {
        return charSizeY.toFloat()
    }

    override fun generateTexture(
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int,
        backgroundColor: Int
    ) {
        getImageStack { stack, err ->
            if (stack != null) {
                val image = if (text.length == 1) {
                    generateTexture(text[0], stack)
                } else {
                    val length = getWrittenLength(text, widthLimit)
                    val width = length * charSizeX
                    val height = charSizeY
                    val image = IntImage(width, height, false)
                    val codepoints = text.codepoints(length)
                    for (i in 0 until length) {
                        generateTexture(codepoints[i], stack).copyInto(image, i * charSizeX, 0)
                    }
                    image
                }
                image.createTexture(
                    Texture2D(text.toString(), image.width, image.height, 1),
                    checkRedundancy = false, callback
                )
            } else callback.err(err)
        }
    }

    override fun generateASCIITexture(
        portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int,
        backgroundColor: Int
    ) {
        getImageStack { stack, err ->
            if (stack != null) {
                Texture2DArray("awtAtlas", charSizeX, charSizeY, simpleChars.size)
                    .create(simpleChars.map { generateTexture(it[0], stack) }, false, callback)
            } else callback.err(err)
        }
    }
}