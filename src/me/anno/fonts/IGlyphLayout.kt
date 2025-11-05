package me.anno.fonts

abstract class IGlyphLayout {

    var width = 0
    var height = 0
    var size = 0
    var numLines = 0

    open fun clear() {
        width = 0
        height = 0
        size = 0
        numLines = 0
    }

    abstract fun add(
        codepoint: Int,
        x0: Int, x1: Int,
        lineIndex: Int, fontIndex: Int
    )

    abstract fun move(dx: Int, deltaLineWidth: Int)
    abstract fun finishLine(i0: Int, i1: Int, lineWidth: Int)

}