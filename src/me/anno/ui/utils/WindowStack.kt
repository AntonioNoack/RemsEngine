package me.anno.ui.utils

import me.anno.gpu.Window
import me.anno.ui.base.Panel
import java.util.*

class WindowStack : Stack<Window>() {

    fun push(panel: Panel) {
        push(Window(panel, this))
    }

    fun push(panel: Panel, fullscreen: Boolean, x: Int, y: Int) {
        push(Window(panel, fullscreen, this, x, y))
    }

    fun getPanelAndWindowAt(x: Float, y: Float) =
        getPanelAndWindowAt(x.toInt(), y.toInt())

    fun getPanelAndWindowAt(x: Int, y: Int): Pair<Panel, Window>? {
        for (index in size - 1 downTo 0) {
            val root = this[index]
            val panel = root.panel.getPanelAt(x, y)
            if (panel != null) return panel to root
        }
        return null
    }

    fun getPanelAt(x: Float, y: Float) = getPanelAt(x.toInt(), y.toInt())
    fun getPanelAt(x: Int, y: Int): Panel? {
        for (i in size - 1 downTo 0) {
            val root = this[i]
            val panel = root.panel.getPanelAt(x, y)
            if (panel != null) return panel
        }
        return null
    }

}