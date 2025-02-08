package me.anno.ui.base.progress

import me.anno.Time
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.a
import kotlin.math.min

class ProgressBarPanel(name: String, unit: String, total: Double, val height1: Int, style: Style) : Panel(style) {

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
        minH = height1
    }

    val progressBar = ProgressBar(name, unit, total)
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        if (min(progressBar.textColor.a(), progressBar.backgroundColor.a()) < 255)
            super.draw(x0, y0, x1, y1) // else no background needed
        progressBar.draw(x, y, width, height1, x0, y0, x1, y1, Time.nanoTime)
    }

}