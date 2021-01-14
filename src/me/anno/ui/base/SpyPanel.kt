package me.anno.ui.base

import me.anno.ui.style.Style

open class SpyPanel(style: Style, val update: () -> Unit): Panel(style) {
    override fun tickUpdate() {
        super.tickUpdate()
        update()
    }
    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(1,1)
        minW = 1
        minH = 1
    }
}