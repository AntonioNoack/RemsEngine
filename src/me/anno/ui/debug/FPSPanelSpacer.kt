package me.anno.ui.debug

import me.anno.engine.WindowRenderFlags
import me.anno.ui.Panel
import me.anno.ui.Style

/**
 * Add this to a list (of buttons with weights) as the last element to avoid the rest being covered by the FrameTimes.
 * */
class FPSPanelSpacer(style: Style) : Panel(style) {
    override fun calculateSize(w: Int, h: Int) {
        val window = window
        val showFPS = WindowRenderFlags.showFPS
        val ws = window?.windowStack
        minW = if (ws != null && showFPS) {
            val checkedPanel = this
            val paddingX = 4
            val moveX = (checkedPanel.x + checkedPanel.width) - (ws.width - (FrameTimings.width + paddingX))
            val moveY = (checkedPanel.y + checkedPanel.height) - (ws.height - FrameTimings.height)
            if (moveX > 0 && moveY > 0) moveX else 0
        } else {
            if (WindowRenderFlags.showFPS) FrameTimings.width else 0
        }
        minH = 1
    }

    override fun clone(): FPSPanelSpacer {
        val clone = FPSPanelSpacer(style)
        copyInto(clone)
        return clone
    }
}