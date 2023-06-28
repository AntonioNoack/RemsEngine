package me.anno.ui.base.progress

import me.anno.Engine
import me.anno.ui.Panel
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import kotlin.math.min

class ProgressBarPanel(name: String, unit: String, total: Double, val height: Int, style: Style) : Panel(style) {

    var progress
        get() = progressBar.progress
        set(value) {
            if (progressBar.progress != value) {
                progressBar.progress = value
                invalidateDrawing()
            }
        }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = height
    }

    val progressBar = ProgressBar(name, unit, total)
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (min(progressBar.color.a(), progressBar.backgroundColor.a()) < 255)
            super.onDraw(x0, y0, x1, y1) // else no background needed
        progressBar.draw(x, y, w, h, x0, y0, x1, y1, Engine.gameTime)
    }

}