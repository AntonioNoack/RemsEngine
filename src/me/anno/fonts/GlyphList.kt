package me.anno.fonts

import kotlin.math.max

open class GlyphList(capacity: Int) {

    var size = 0
        private set

    val indices get() = 0 until size

    fun isEmpty() = size == 0

    private var ints = IntArray(capacity * 3)
    private var floats = FloatArray(capacity * 4)

    fun add(
        codepoint: Int,
        x0: Float, x1: Float, lineWidth: Float,
        y: Float, lineIndex: Int, fontIndex: Int
    ) {
        val glyphIndex = size
        if (glyphIndex * 3 >= ints.size) {
            val newSize = max(16, glyphIndex * 2)
            floats = floats.copyOf(newSize * 4)
            ints = ints.copyOf(newSize * 3)
        }

        val floats = floats
        val floatOffset = glyphIndex * 4
        floats[floatOffset] = x0
        floats[floatOffset + 1] = x1
        floats[floatOffset + 2] = y
        floats[floatOffset + 3] = lineWidth

        val ints = ints
        ints[glyphIndex * 3] = codepoint
        ints[glyphIndex * 3 + 1] = lineIndex
        ints[glyphIndex * 3 + 2] = fontIndex
        size = glyphIndex + 1
    }

    fun getX0(glyphIndex: Int) = floats[glyphIndex * 4]
    fun getX1(glyphIndex: Int) = floats[glyphIndex * 4 + 1]
    fun getY(glyphIndex: Int) = floats[glyphIndex * 4 + 2]
    fun getLineWidth(glyphIndex: Int) = floats[glyphIndex * 4 + 3]
    fun setLineWidth(glyphIndex: Int, lineWidth: Float) {
        floats[glyphIndex * 4 + 3] = lineWidth
    }

    fun getCodepoint(glyphIndex: Int) = ints[glyphIndex * 3]

    @Suppress("unused")
    fun getLineIndex(glyphIndex: Int) = ints[glyphIndex * 3 + 1]
    fun getFontIndex(glyphIndex: Int) = ints[glyphIndex * 3 + 2]

}