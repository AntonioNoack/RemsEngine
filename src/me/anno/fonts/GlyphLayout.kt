package me.anno.fonts

import me.anno.cache.ICacheData
import me.anno.gpu.drawing.GFXx2D
import kotlin.math.min

open class GlyphLayout(
    val font: Font, val text: CharSequence,
    val relativeWidthLimit: Float, val maxNumLines: Int
) : GlyphList(text.length), ICacheData {


    val actualFontSize: Float

    /**
     * Multiply coordinates and sizes by this value to get units in [0,1] instead of [0,fontSize]
     * */
    val baseScale: Float = 1f / font.size

    init {
        val fontImpl = FontManager.getFontImpl()
        actualFontSize = fontImpl.getLineHeight(font)
        fontImpl.fillGlyphLayout(font, text, this, relativeWidthLimit, maxNumLines)
    }

    fun getSizeX(widthLimit: Int): Int {
        return min(width, widthLimit)
    }

    fun getSizeY(heightLimit: Int): Int {
        return min(height, heightLimit)
    }

    fun getSize(widthLimit: Int, heightLimit: Int): Int {
        val width = getSizeX(widthLimit)
        val height = getSizeY(heightLimit)
        return GFXx2D.getSize(width, height)
    }
}