package me.anno.fonts

import me.anno.cache.ICacheData
import me.anno.gpu.drawing.GFXx2D
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil
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

    var width: Float = 0f
    var height: Float = 0f
    var numLines: Int = 0

    init {
        val fontImpl = FontManager.getFontImpl()
        actualFontSize = fontImpl.getLineHeight(font)
        fontImpl.fillGlyphLayout(this, relativeWidthLimit, maxNumLines)
    }

    fun getSizeX(widthLimit: Int): Int {
        return min(ceil(width).toIntOr(), widthLimit)
    }

    fun getSizeY(heightLimit: Int): Int {
        return min(ceil(height).toIntOr(), heightLimit)
    }

    fun getSize(widthLimit: Int, heightLimit: Int): Int {
        val width = getSizeX(widthLimit)
        val height = getSizeY(heightLimit)
        return GFXx2D.getSize(width, height)
    }
}