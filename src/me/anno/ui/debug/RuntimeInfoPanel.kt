package me.anno.ui.debug

import me.anno.Engine.gameTime
import me.anno.audio.AudioPools
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.language.translation.Dict
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.style.Style
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager
import kotlin.math.abs

class RuntimeInfoPanel(style: Style) : SimpleTextPanel(style) {

    var updateInterval = 100_000_000
    var lastUpdate = 0L

    override fun onUpdate() {
        super.onUpdate()
        val time = gameTime
        if (abs(time - lastUpdate) > updateInterval) {
            text = getDebugText()
            lastUpdate = time
            invalidateLayout()
        }
    }

    // to do another slot may be interesting: how much ram is used by sub-processes
    // I don't know how to implement that... we'd need to track, which sub-processes are alive,
    // and get their process id, so we can ask the OS about RAM usage

    private fun getDebugText(): String {
        val runtime = Runtime.getRuntime()
        val memory = runtime.totalMemory() - runtime.freeMemory()
        val videoMemory = Texture2D.allocated + CubemapTexture.allocated + Texture3D.allocated + OpenGLBuffer.allocated
        return if (OS.isWeb) {
            // can we query how much memory JS is using?
            // https://stackoverflow.com/questions/2530228/jquery-or-javascript-to-find-memory-usage-of-page
            // window.performance.memory ; might be Chrome-only -> edge works; doesn't work in Firefox
            // todo query and display this somehow...
            Dict["JVM/C/VRAM: %1/%2 MB", "ui.debug.ramUsage2"]
                .replace("%1", format1(memory))
                .replace("%2", format1(videoMemory))
        } else {
            val cMemory = ByteBufferPool.getAllocated()
            Dict["JVM/C/VRAM: %1/%3/%2 MB", "ui.debug.ramUsage2"]
                .replace("%1", format1(memory))
                .replace("%2", format1(videoMemory))
                .replace("%3", format1(cMemory))
        }
    }

    private fun format1(size: Long) = (size.toFloat() / (1 shl 20)).f1()

    fun printDetailedReport() {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        // todo align numbers
        // todo totals for groups
        LOGGER.debug(
            "" +
                    "JVM:\n" +
                    "  Total:  ${format1(total)} MB\n" +
                    "  Used:   ${format1(total - runtime.freeMemory())} MB\n" +
                    "  Free:   ${format1(runtime.freeMemory())} MB\n" +
                    "JVM-Buffers:\n" +
                    "  Bytes:  ${format1(Texture2D.byteArrayPool.totalSize)} MB\n" +
                    "  Shorts: ${format1(AudioPools.SAPool.totalSize)} MB\n" +
                    "  Ints:   ${format1(Texture2D.intArrayPool.totalSize)} MB\n" +
                    "  Floats: ${format1(Texture2D.floatArrayPool.totalSize + AudioPools.FAPool.totalSize)} MB\n" +
                    "VRAM:\n" +
                    "  Texture2d:      ${format1(Texture2D.allocated)} MB\n" +
                    "  Texture3d:      ${format1(Texture3D.allocated)} MB\n" +
                    "  TextureCubemap: ${format1(CubemapTexture.allocated)} MB\n" +
                    "  Geometry:       ${format1(OpenGLBuffer.allocated)} MB\n" +
                    "Native:   ${format1(ByteBufferPool.getAllocated())} MB"
        )
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        if (key == '?'.code) {
            printDetailedReport()
        } else super.onCharTyped(x, y, key)
    }

    override fun acceptsChar(char: Int): Boolean {
        return char == '?'.code
    }

    init {
        text = getDebugText()
        textColor = textColor and 0x7fffffff
        backgroundColor = backgroundColor and 0xffffff
        add(WrapAlign.RightBottom)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(RuntimeInfoPanel::class)
    }

}