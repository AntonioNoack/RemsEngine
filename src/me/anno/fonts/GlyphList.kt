package me.anno.fonts

import kotlin.math.max

open class GlyphList(capacity: Int) {

    var size = 0
        private set

    val indices get() = 0 until size

    fun isEmpty() = size == 0

    private var ints = IntArray(capacity * 7)

    fun add(
        codepoint: Int,
        x0: Int, x1: Int, lineWidth: Int,
        y: Int, lineIndex: Int, fontIndex: Int
    ) {
        val glyphIndex = size
        if (glyphIndex * 7 >= ints.size) {
            val newSize = max(16, glyphIndex * 7)
            ints = ints.copyOf(newSize * 7)
        }

        val ints = ints
        val di = glyphIndex * 7
        ints[di] = codepoint
        ints[di + 1] = lineIndex
        ints[di + 2] = fontIndex
        ints[di + 3] = x0
        ints[di + 4] = x1
        ints[di + 5] = y
        ints[di + 6] = lineWidth
        size = glyphIndex + 1
    }

    fun getX0(glyphIndex: Int) = ints[glyphIndex * 7 + 3]
    fun getX1(glyphIndex: Int) = ints[glyphIndex * 7 + 4]
    fun getY(glyphIndex: Int) = ints[glyphIndex * 7 + 5]
    fun getLineWidth(glyphIndex: Int) = ints[glyphIndex * 7 + 6]
    fun setLineWidth(glyphIndex: Int, lineWidth: Int) {
        ints[glyphIndex * 7 + 6] = lineWidth
    }

    fun getCodepoint(glyphIndex: Int) = ints[glyphIndex * 7]

    @Suppress("unused")
    fun getLineIndex(glyphIndex: Int) = ints[glyphIndex * 7 + 1]
    fun getFontIndex(glyphIndex: Int) = ints[glyphIndex * 7 + 2]

    fun move(dx: Int, dy: Int, deltaLineWidth: Int) {
        val ints = ints
        for (i in 0 until size) {
            val di = i * 7
            ints[di + 3] += dx
            ints[di + 4] += dx
            ints[di + 5] += dy
            ints[di + 6] += deltaLineWidth
        }
    }

}