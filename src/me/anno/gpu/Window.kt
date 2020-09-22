package me.anno.gpu

import me.anno.input.MouseButton
import me.anno.ui.base.Panel

class Window(val panel: Panel, val isFullscreen: Boolean, val x: Int, val y: Int){

    fun setAcceptsClickAway(boolean: Boolean) {
        acceptsClickAway = { boolean }
    }

    var acceptsClickAway = { button: MouseButton -> true }
}