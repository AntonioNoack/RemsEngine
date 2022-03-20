package me.anno.ui.debug

import me.anno.Engine.gameTime
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.language.translation.Dict
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.style.Style
import me.anno.utils.pooling.ByteBufferPool
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

    // todo another slot may be interesting: how much ram is used by sub-processes

    // todo onclick show more details about the ram and vram usage, e.g. allocated textures, with and without multisampling, ...
    // todo also we could plot them in a graph
    private fun getDebugText(): String {
        val runtime = Runtime.getRuntime()
        val memory = runtime.totalMemory() - runtime.freeMemory()
        val videoMemory = Texture2D.allocated + CubemapTexture.allocated + Texture3D.allocated + Buffer.allocated
        val cMemory = ByteBufferPool.getAllocated()
        return Dict["JVM/C/VRAM: %1/%3/%2 MB", "ui.debug.ramUsage2"]
            .replace("%1", (memory.toFloat() / (1 shl 20)).f1())
            .replace("%2", (videoMemory.toFloat() / (1 shl 20)).f1())
            .replace("%3", (cMemory.toFloat() / (1 shl 20)).f1())
    }

    init {
        text = getDebugText()
        textColor = textColor and 0x7fffffff
        backgroundColor = backgroundColor and 0xffffff
        add(WrapAlign.RightBottom)
    }

}