package me.anno.ui.debug

import me.anno.gpu.GFX.gameTime
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.texture.Texture2D
import me.anno.language.translation.Dict
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.style.Style
import me.anno.utils.types.Floats.f1
import kotlin.math.abs

class RuntimeInfoPanel(style: Style) : SimpleTextPanel(style) {

    var updateInterval = 500_000_000
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
        val memory = runtime.totalMemory() - runtime.freeMemory()
        val videoMemory = Texture2D.allocated + Buffer.allocated
        return Dict["RAM/VRAM: %1/%2 MB", "ui.debug.ramUsage2"]
            .replace("%1", (memory.toFloat()/(1 shl 20)).f1())
            .replace("%2", (videoMemory.toFloat()/(1 shl 20)).f1())
    }

    init {
        text = getDebugText()
        textColor = textColor and 0x7fffffff
        add(WrapAlign.RightBottom)
    }

}