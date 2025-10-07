package me.anno.fonts

import me.anno.cache.ICacheData
import me.anno.fonts.CharacterOffsetCache.Companion.getOffsetCache

open class GlyphLayout(
    val font: Font, val text: CharSequence,
    relativeWidthLimit: Float, maxNumLines: Int
) : GlyphList(text.length), ICacheData {

    private val offsetCache = getOffsetCache(font)

    val actualFontSize: Float
    val baseScale: Float = 1f / font.size

    var width: Float = 0f
    var height: Float = 0f
    var numLines: Int = 0

    init {
        val fontImpl = FontManager.getFont(font)
        actualFontSize = fontImpl.getActualFontHeight()
        fontImpl.fillGlyphLayout(this, relativeWidthLimit, maxNumLines, offsetCache)
    }

    val meshCache get() = offsetCache.charMesh

}