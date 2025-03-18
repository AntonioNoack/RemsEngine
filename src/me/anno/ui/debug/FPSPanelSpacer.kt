package me.anno.ui.debug

import me.anno.engine.WindowRenderFlags
import me.anno.ui.Panel
import me.anno.ui.Style
import kotlin.math.max

/**
 * Add this to a list (of buttons with weights) as the last element to avoid the rest being covered by the FrameTimes.
 * */
class FPSPanelSpacer(style: Style) : Panel(style) {
    override fun calculateSize(w: Int, h: Int) {
        val window = window
        val showFPS = WindowRenderFlags.showFPS
        val ws = window?.windowStack
        minW = if (ws != null && showFPS) {
            // todo respect height for this calculation, too: we don't need to move out the way,
            //  if we're above (e.g. on welcome screen)
            val gap = ws.width - (window.panel.x + window.panel.width)
            max(FrameTimings.width - gap, 0)
        } else {
            if (WindowRenderFlags.showFPS) FrameTimings.width else 0
        }
        minH = 1
    }
}