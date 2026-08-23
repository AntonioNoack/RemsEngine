package me.anno.ui.base.progress

import me.anno.Time
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.utils.Color.a
import kotlin.math.min

open class ProgressBarPanel(name: String, unit: String, total: Double, val minHeight: Int, style: Style) : Panel(style) {

    var progress
        get() = progressBar.progress
        set(value) {
            progressBar.progress = value
        }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        minH = minHeight
    }

    val progressBar = ProgressBar(name, unit, total)
    override fun draw(canvas: Canvas) {
        if (min(progressBar.textColor.a(), progressBar.backgroundColor.a()) < 255)
            super.draw(canvas) // else no background needed
        progressBar.draw(x, y, width, minHeight, x0, y0, x1, y1, Time.nanoTime)
    }

}