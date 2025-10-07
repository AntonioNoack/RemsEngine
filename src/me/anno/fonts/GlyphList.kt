package me.anno.fonts

import kotlin.math.max

open class GlyphList(capacity: Int) {

    var size = 0
        private set

    val indices get() = 0 until size

    fun isEmpty() = size == 0

    private var ints = IntArray(capacity * 2)
    private var floats = FloatArray(capacity * 4)

    fun add(
        codepoint: Int,
        x0: Float, x1: Float, lineWidth: Float,
        y: Float, lineIndex: Int,
    ) {
        val glyphIndex = size
        if (glyphIndex * 2 >= ints.size) {
            val newSize = max(16, glyphIndex * 2)
            floats = floats.copyOf(newSize * 4)
            ints = ints.copyOf(newSize)
        }

        val floats = floats
        val floatOffset = glyphIndex * 4
        floats[floatOffset] = x0
        floats[floatOffset + 1] = x1
        floats[floatOffset + 2] = y
        floats[floatOffset + 3] = lineWidth

        val ints = ints
        ints[glyphIndex * 2] = codepoint
        ints[glyphIndex * 2 + 1] = lineIndex
        size = glyphIndex + 1
    }

    fun getX0(glyphIndex: Int) = floats[glyphIndex * 4]
    fun getX1(glyphIndex: Int) = floats[glyphIndex * 4 + 1]
    fun getY(glyphIndex: Int) = floats[glyphIndex * 4 + 2]
    fun getLineWidth(glyphIndex: Int) = floats[glyphIndex * 4 + 3]
    fun getCodepoint(glyphIndex: Int) = ints[glyphIndex * 2]
    fun getLineIndex(glyphIndex: Int) = ints[glyphIndex * 2 + 1]
}