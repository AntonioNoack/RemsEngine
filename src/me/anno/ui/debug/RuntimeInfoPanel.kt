package me.anno.ui.debug

import me.anno.gpu.GFX.gameTime
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style
import me.anno.utils.FloatFormat.f1
import kotlin.math.abs

class RuntimeInfoPanel(style: Style) : TextPanel("", style) {

    var updateInterval = 2000_000_000
    var lastUpdate = 0L

    override fun tickUpdate() {
        super.tickUpdate()
        val time = gameTime
        if (abs(time - lastUpdate) > updateInterval) {
            text = getDebugText()
            lastUpdate = time
            invalidateLayout()
        }
    }

    private fun getDebugText(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)
        return "Memory: ${usedMemory.f1()} MiB"
    }

    init {
        text = getDebugText()
        font = font.withSize(font.size * 2 / 3)
        textColor = textColor and 0x7fffffff
        add(WrapAlign.RightBottom)
    }

}