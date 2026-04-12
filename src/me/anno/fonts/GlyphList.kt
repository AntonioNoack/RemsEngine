package me.anno.fonts

import me.anno.maths.Packing.pack64
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.utils.types.Booleans.hasFlag
import kotlin.math.max

open class GlyphList(capacity: Int) : IGlyphLayout() {

    companion object {
        private const val NUM_INTS = 8
        private const val ATTR_CODEPOINT = 0
        private const val ATTR_LINE_INDEX = 1
        private const val ATTR_FONT_INDEX = 2
        private const val ATTR_X0 = 3
        private const val ATTR_X1 = 4
        private const val ATTR_LINE_WIDTH = 5
        private const val ATTR_STYLE_0 = 6
        private const val ATTR_STYLE_1 = 7
    }

    val indices get() = 0 until size

    fun isEmpty() = size == 0

    private var values = IntArray(capacity * NUM_INTS)

    override fun add(codepoint: Int, x0: Int, x1: Int, lineIndex: Int, fontIndex: Int, style: Long) {
        val glyphIndex = size++
        if (glyphIndex * NUM_INTS >= values.size) {
            val newSize = max(16, glyphIndex * NUM_INTS)
            values = values.copyOf(newSize * NUM_INTS)
        }

        val values = values
        val di = glyphIndex * NUM_INTS
        values[di + ATTR_CODEPOINT] = codepoint
        values[di + ATTR_LINE_INDEX] = lineIndex
        values[di + ATTR_FONT_INDEX] = fontIndex
        values[di + ATTR_X0] = x0
        values[di + ATTR_X1] = x1
        values[di + ATTR_STYLE_0] = unpackHighFrom64(style)
        values[di + ATTR_STYLE_1] = unpackLowFrom64(style)
    }

    fun getX0(glyphIndex: Int) = values[glyphIndex * NUM_INTS + ATTR_X0]
    fun getX1(glyphIndex: Int) = values[glyphIndex * NUM_INTS + ATTR_X1]
    fun getY(glyphIndex: Int, font: Font) = getLineIndex(glyphIndex) * font.lineSpacingI
    fun getLineWidth(glyphIndex: Int) = values[glyphIndex * NUM_INTS + ATTR_LINE_WIDTH]
    fun setLineWidth(glyphIndex: Int, lineWidth: Int) {
        values[glyphIndex * NUM_INTS + ATTR_LINE_WIDTH] = lineWidth
    }

    fun getCodepoint(glyphIndex: Int) = values[glyphIndex * NUM_INTS + ATTR_CODEPOINT]

    @Suppress("unused")
    fun getLineIndex(glyphIndex: Int) = values[glyphIndex * NUM_INTS + ATTR_LINE_INDEX]

    /**
     * returns trueFontIndex * 4 + isBold * GlyphStyle.BOLD + isItalic * GlyphStyle.ITALIC
     * */
    fun getFontIndex(glyphIndex: Int) = values[glyphIndex * NUM_INTS + ATTR_FONT_INDEX]

    /**
     * returns index into fallback fonts list, 0 = user-defined font
     * */
    fun getTrueFontIndex(glyphIndex: Int) = getFontIndex(glyphIndex) shr 2

    fun isBold(glyphIndex: Int) = getFontIndex(glyphIndex).hasFlag(GlyphStyle.BOLD)
    fun isItalic(glyphIndex: Int) = getFontIndex(glyphIndex).hasFlag(GlyphStyle.ITALIC)
    fun isUnderline(glyphIndex: Int) = getCodepoint(glyphIndex) == GlyphStyle.UNDERLINE_CHAR.code
    fun isStrikethrough(glyphIndex: Int) = getCodepoint(glyphIndex) == GlyphStyle.STRIKETHROUGH_CHAR.code
    fun isDecoration(glyphIndex: Int): Boolean = isUnderline(glyphIndex) || isStrikethrough(glyphIndex)

    fun getStyle(glyphIndex: Int): Long {
        val high = values[glyphIndex * NUM_INTS + ATTR_STYLE_0]
        val low = values[glyphIndex * NUM_INTS + ATTR_STYLE_1]
        return pack64(high, low)
    }

    override fun move(dx: Int, deltaLineWidth: Int) {
        val ints = values
        for (i in 0 until size) {
            val di = i * NUM_INTS
            ints[di + ATTR_X0] += dx
            ints[di + ATTR_X1] += dx
            ints[di + ATTR_LINE_WIDTH] += deltaLineWidth
        }
    }

    override fun finishLine(i0: Int, i1: Int, lineWidth: Int) {
        for (i in i0 until i1) {
            setLineWidth(i, lineWidth)
        }
    }

}