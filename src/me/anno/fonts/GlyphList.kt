package me.anno.fonts

import kotlin.math.max

open class GlyphList(capacity: Int) : IGlyphLayout() {

    companion object {
        private const val NUM_INTS = 6
        private const val ATTR_CODEPOINT = 0
        private const val ATTR_LINE_INDEX = 1
        private const val ATTR_FONT_INDEX = 2
        private const val ATTR_X0 = 3
        private const val ATTR_X1 = 4
        private const val ATTR_LINE_WIDTH = 5
    }

    val indices get() = 0 until size

    fun isEmpty() = size == 0

    private var ints = IntArray(capacity * NUM_INTS)

    override fun add(codepoint: Int, x0: Int, x1: Int, lineIndex: Int, fontIndex: Int) {
        val glyphIndex = size++
        if (glyphIndex * NUM_INTS >= ints.size) {
            val newSize = max(16, glyphIndex * NUM_INTS)
            ints = ints.copyOf(newSize * NUM_INTS)
        }

        val ints = ints
        val di = glyphIndex * NUM_INTS
        ints[di + ATTR_CODEPOINT] = codepoint
        ints[di + ATTR_LINE_INDEX] = lineIndex
        ints[di + ATTR_FONT_INDEX] = fontIndex
        ints[di + ATTR_X0] = x0
        ints[di + ATTR_X1] = x1
    }

    fun getX0(glyphIndex: Int) = ints[glyphIndex * NUM_INTS + ATTR_X0]
    fun getX1(glyphIndex: Int) = ints[glyphIndex * NUM_INTS + ATTR_X1]
    fun getY(glyphIndex: Int, font: Font) = getLineIndex(glyphIndex) * font.lineHeightI
    fun getLineWidth(glyphIndex: Int) = ints[glyphIndex * NUM_INTS + ATTR_LINE_WIDTH]
    fun setLineWidth(glyphIndex: Int, lineWidth: Int) {
        ints[glyphIndex * NUM_INTS + ATTR_LINE_WIDTH] = lineWidth
    }

    fun getCodepoint(glyphIndex: Int) = ints[glyphIndex * NUM_INTS + ATTR_CODEPOINT]

    @Suppress("unused")
    fun getLineIndex(glyphIndex: Int) = ints[glyphIndex * NUM_INTS + ATTR_LINE_INDEX]
    fun getFontIndex(glyphIndex: Int) = ints[glyphIndex * NUM_INTS + ATTR_FONT_INDEX]

    override fun move(dx: Int, deltaLineWidth: Int) {
        val ints = ints
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