package me.anno.ui.input.components

import me.anno.ui.Style

class EmptyColorPalette(style: Style) : ColorPalette(1, 1, style) {
    override fun setColor(x: Int, y: Int, color: Int) {}
    override fun getColor(x: Int, y: Int) = 0
    override fun clone(): ColorPalette {
        return EmptyColorPalette(style)
    }
}