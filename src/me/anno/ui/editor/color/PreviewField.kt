package me.anno.ui.editor.color

import me.anno.gpu.GFXx2D
import me.anno.ui.base.Panel
import me.anno.ui.style.Style

class PreviewField(private val refSize: Panel, val padding: Int, style: Style) : Panel(style) {
    var color = 0
    override fun calculateSize(w: Int, h: Int) {
        val size = refSize.minH
        minW = size
        minH = size
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        GFXx2D.drawRect(x + padding, y + padding, w - 2 * padding, h - 2 * padding, color)
    }
}