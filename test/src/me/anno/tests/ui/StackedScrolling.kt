package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.black

fun main() {
    testUI3("StackedScrolling", create(false, 8))
}

fun create(x: Boolean, depth: Int): Panel {
    if (depth == 0) {
        return object : Panel(style) {
            override fun calculateSize(w: Int, h: Int) {
                minW = 20
                minH = 20
            }
        }.apply {
            backgroundColor = (Maths.random() * 1e9).toInt() or black
        }
    } else {
        val p = if (x) {
            ScrollPanelX(style)
        } else {
            ScrollPanelY(style)
        }
        for (i in 0 until 5) {
            (p.child as PanelList).add(create(!x, depth - 1))
        }
        return p
    }
}