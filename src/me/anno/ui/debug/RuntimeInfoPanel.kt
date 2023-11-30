package me.anno.ui.debug

import me.anno.Time.nanoTime
import me.anno.audio.AudioPools
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.language.translation.Dict
import me.anno.ui.base.text.SimpleTextPanel
import me.anno.ui.debug.JSMemory.jsUsedMemory
import me.anno.ui.Style
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.utils.Color.withAlpha
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
        val time = nanoTime
        if (abs(time - lastUpdate) > updateInterval) {
            val newText = getDebugText()
            lastUpdate = time
            if (text != newText) {
                text = newText
                invalidateDrawing()
            }
        }
    }

    // to do another slot may be interesting: how much ram is used by sub-processes
    // I don't know how to implement that... we'd need to track, which sub-processes are alive,
    // and get their process id, so we can ask the OS about RAM usage

    private fun getDebugText(): String {
        val runtime = Runtime.getRuntime()
        val jvmMemory = runtime.totalMemory() - runtime.freeMemory()
        val videoMemory =
            Texture2D.allocated + CubemapTexture.allocated + Texture3D.allocated + OpenGLBuffer.allocated +
                    Texture2DArray.allocated
        return if (OS.isWeb) {
            val jsMemory = jsUsedMemory() - jvmMemory
            if (jsMemory >= 0) {
                Dict["JVM/JS/VRAM: %1/%3/%2 MB", "ui.debug.ramUsage4"]
                    .replace("%1", format1(jvmMemory))
                    .replace("%2", format1(videoMemory))
                    .replace("%3", format1(jsMemory))
            } else {
                Dict["JVM/VRAM: %1/%2 MB", "ui.debug.ramUsage3"]
                    .replace("%1", format1(jvmMemory))
                    .replace("%2", format1(videoMemory))
            }
        } else {
            val cMemory = ByteBufferPool.getAllocated()
            Dict["JVM/C/VRAM: %1/%3/%2 MB", "ui.debug.ramUsage2"]
                .replace("%1", format1(jvmMemory))
                .replace("%2", format1(videoMemory))
                .replace("%3", format1(cMemory))
        }
    }

    private fun format1(size: Long) = (size.toFloat() / (1 shl 20)).f1()

    fun printDetailedReport() {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        // todo align numbers
        val jvmUsed = total - runtime.freeMemory()
        val vramTotal = Texture2D.allocated + Texture3D.allocated +
                CubemapTexture.allocated + OpenGLBuffer.allocated
        val jvmBufferTotal =
            Texture2D.byteArrayPool.totalSize + AudioPools.SAPool.totalSize + Texture2D.intArrayPool.totalSize +
                    Texture2D.floatArrayPool.totalSize + AudioPools.FAPool.totalSize
        val native = ByteBufferPool.getAllocated()
        LOGGER.debug(
            "" +
                    "JVM:\n" +
                    "  Used:   ${format1(jvmUsed)} MB\n" +
                    "  Free:   ${format1(runtime.freeMemory())} MB\n" +
                    "  Total:  ${format1(total)} MB\n" +
                    "JVM-Buffers:\n" +
                    "  Bytes:  ${format1(Texture2D.byteArrayPool.totalSize)} MB\n" +
                    "  Shorts: ${format1(AudioPools.SAPool.totalSize)} MB\n" +
                    "  Ints:   ${format1(Texture2D.intArrayPool.totalSize)} MB\n" +
                    "  Floats: ${format1(Texture2D.floatArrayPool.totalSize + AudioPools.FAPool.totalSize)} MB\n" +
                    "  Total:  ${format1(jvmBufferTotal)} MB\n" +
                    "VRAM:\n" +
                    "  Texture2d:      ${format1(Texture2D.allocated)} MB\n" +
                    "  Texture3d:      ${format1(Texture3D.allocated)} MB\n" +
                    "  TextureCubemap: ${format1(CubemapTexture.allocated)} MB\n" +
                    "  Geometry:       ${format1(OpenGLBuffer.allocated)} MB\n" +
                    "  Total:          ${format1(vramTotal)} MB\n" +
                    "Native:   ${format1(native)} MB\n" +
                    "Total:    ${format1(native + vramTotal + jvmBufferTotal + jvmUsed)} MB"
        )
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        if (codepoint == '?'.code) {
            printDetailedReport()
        } else super.onCharTyped(x, y, codepoint)
    }

    override fun acceptsChar(char: Int): Boolean {
        return char == '?'.code
    }

    init {
        text = getDebugText()
        textColor = textColor.withAlpha(0x7f)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(RuntimeInfoPanel::class)
    }

}