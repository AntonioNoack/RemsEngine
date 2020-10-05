package me.anno.gpu

import me.anno.input.MouseButton
import me.anno.ui.base.Panel

class Window (val panel: Panel, val isFullscreen: Boolean, val x: Int, val y: Int){

    constructor(panel: Panel): this(panel, true, 0, 0)
    constructor(panel: Panel, x: Int, y: Int): this(panel, false, x, y)

    fun setAcceptsClickAway(boolean: Boolean) {
        acceptsClickAway = { boolean }
    }

    var acceptsClickAway = { button: MouseButton -> true }
}