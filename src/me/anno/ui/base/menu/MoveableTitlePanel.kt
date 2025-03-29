package me.anno.ui.base.menu

import me.anno.input.Input
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Style
import me.anno.ui.base.text.TextPanel
import me.anno.utils.types.Floats.roundToIntOr

class MoveableTitlePanel(title: NameDesc, style: Style) : TextPanel(title, style) {
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        val window = window
        if (Input.isLeftDown && window != null) {
            // move the window
            window.x = clamp(
                window.x + dx.roundToIntOr(),
                0, windowStack.width - window.panel.width
            )
            window.y = clamp(
                // we only can control the window at the top -> top needs to stay visible
                window.y + dy.roundToIntOr(), 0,
                windowStack.height - window.panel.height
            )
        } else super.onMouseMoved(x, y, dx, dy)
    }
}