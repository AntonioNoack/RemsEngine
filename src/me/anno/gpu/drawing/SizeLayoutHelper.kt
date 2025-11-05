package me.anno.gpu.drawing

import me.anno.fonts.IGlyphLayout

class SizeLayoutHelper : IGlyphLayout() {
    override fun add(
        codepoint: Int, x0: Int, x1: Int,
        lineIndex: Int, fontIndex: Int
    ) {
        size++
    }

    override fun move(dx: Int, deltaLineWidth: Int) {}
    override fun finishLine(i0: Int, i1: Int, lineWidth: Int) {}
}