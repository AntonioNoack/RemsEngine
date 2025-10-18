package me.anno.fonts

import me.anno.cache.Promise
import me.anno.cache.CacheSection
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.clamp
import me.anno.utils.OS.res
import me.anno.utils.async.Callback
import me.anno.utils.types.Floats.toIntOr

/**
 * generates a fallback font when other text sources are unavailable using a pre-generated texture;
 * a little blurry, but much better readable than the generator, which only uses lines
 * */
object AtlasFontGenerator : FontImpl<Unit>() {

    private const val NUM_TILES_X = 16
    private const val NUM_TILES_Y = 6
    private val cache = CacheSection<Int, List<IntImage>>("FallbackFontGenerator")

    private fun charSizeY(font: Font) = font.sizeInt
    private fun charSizeX(font: Font) = charSizeY(font) * 7 / 12
    private fun baselineY(font: Font) = charSizeY(font) * 0.73f // measured in Gimp

    private fun getImageStack(font: Font, callback: Callback<List<IntImage>>) {
        cache.getEntry(font.sizeIndex, 10_000) { _, result ->
            val source = res.getChild("textures/ASCIIAtlas.png")
            val charSizeX = charSizeX(font)
            val charSizeY = charSizeY(font)
            ImageCache[source, 50].mapResult(result) { image ->
                image.split(NUM_TILES_X, NUM_TILES_Y).map { tileImage ->
                    tileImage
                        .resized(charSizeX, charSizeY, true)
                        .asIntImage()
                }
            }
        }.waitFor(callback)
    }

    private fun getIndex(codepoint: Int): Int {
        return codepoint - 32
    }

    override fun getBaselineY(font: Font): Float {
        return baselineY(font)
    }

    override fun getLineHeight(font: Font): Float {
        return charSizeY(font).toFloat()
    }

    override fun drawGlyph(
        image: IntImage,
        x0: Float, x1: Float, y0: Float, y1: Float, strictBounds: Boolean,
        font: Font, fallbackFonts: Unit, fontIndex: Int,
        codepoint: Int, textColor: Int, backgroundColor: Int, portableImages: Boolean
    ) {
        val tmp = Promise<List<IntImage>>()
        getImageStack(font, tmp)
        val images = tmp.waitFor()!!
        val charImage = images[clamp(getIndex(codepoint), 0, images.lastIndex)]
        val dx = x0.toIntOr()
        val dy = ((y0 + y1) - charImage.height + 1).toIntOr()
        charImage.copyInto(image, dx, dy)
    }

    override fun getTextLength(font: Font, codepoint: Int): Float {
        return charSizeX(font).toFloat()
    }

    override fun getTextLength(font: Font, codepointA: Int, codepointB: Int): Float {
        return charSizeX(font) * 2f + 1f // 1 for spacing
    }

    override fun getFallbackFonts(font: Font) = Unit
    override fun getSupportLevel(fonts: Unit, codepoint: Int, lastSupportLevel: Int): Int = 0
}