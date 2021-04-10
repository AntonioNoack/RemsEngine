package me.anno.ui.input.components

import me.anno.input.MouseButton
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style

class TitlePanel(title: String, var owner: Panel, style: Style) : TextPanel(title, style) {

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        owner.onMouseDown(x, y, button)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        owner.onMouseUp(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        owner.onMouseMoved(x, y, dx, dy)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        owner.onPaste(x, y, data, type)
    }

    override fun onEmpty(x: Float, y: Float) {
        owner.onEmpty(x, y)
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        return owner.onCopyRequested(x, y)
    }

}