package me.anno.ui.debug

import me.anno.audio.AudioPools
import me.anno.gpu.buffer.GPUBuffer
import me.anno.gpu.texture.CubemapTexture
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.gpu.texture.Texture3D
import me.anno.language.translation.Dict
import me.anno.ui.Style
import me.anno.ui.base.text.UpdatingTextPanel
import me.anno.ui.debug.JSMemory.jsUsedMemory
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.Pools
import me.anno.utils.types.Floats.f1
import org.apache.logging.log4j.LogManager

class RuntimeInfoPanel(style: Style) : UpdatingTextPanel(100, style, ::getDebugText) {

    init {
        textColor = textColor.withAlpha(127)
        disableFocusColors()
    }

    override fun clone(): RuntimeInfoPanel {
        val clone = RuntimeInfoPanel(style)
        copyInto(clone)
        return clone
    }

    companion object {

        private val LOGGER = LogManager.getLogger(RuntimeInfoPanel::class)

        // to do another slot may be interesting: how much ram is used by sub-processes
        // I don't know how to implement that... we'd need to track, which sub-processes are alive,
        // and get their process id, so we can ask the OS about RAM usage

        private fun getDebugText(): String {
            val runtime = Runtime.getRuntime()
            val jvmMemory = runtime.totalMemory() - runtime.freeMemory()
            val videoMemory = getVRAM()
            return if (OS.isWeb) {
                val jsMemory = jsUsedMemory() - jvmMemory
                if (jsMemory >= 0) {
                    Dict["JVM/JS/VRAM: %1/%3/%2 MB", "ui.debug.ramUsage4"]
                        .replace("%1", formatMiB(jvmMemory))
                        .replace("%2", formatMiB(videoMemory))
                        .replace("%3", formatMiB(jsMemory))
                } else {
                    Dict["JVM/VRAM: %1/%2 MB", "ui.debug.ramUsage3"]
                        .replace("%1", formatMiB(jvmMemory))
                        .replace("%2", formatMiB(videoMemory))
                }
            } else {
                val cMemory = ByteBufferPool.getNativeAllocated()
                Dict["JVM/C/VRAM: %1/%3/%2 MB", "ui.debug.ramUsage2"]
                    .replace("%1", formatMiB(jvmMemory))
                    .replace("%2", formatMiB(videoMemory))
                    .replace("%3", formatMiB(cMemory))
            }
        }

        private fun getVRAM(): Long {
            return Texture2D.allocated.get() +
                    Texture3D.allocated.get() +
                    Texture2DArray.allocated.get() +
                    CubemapTexture.allocated.get() +
                    GPUBuffer.allocated.get()
        }

        private fun formatMiB(size: Long) = (size.toFloat() / (1 shl 20)).f1()

        fun printDetailedReport() {
            val runtime = Runtime.getRuntime()
            val total = runtime.totalMemory()
            val jvmUsed = total - runtime.freeMemory()
            val vramTotal = getVRAM()
            val jvmBufferTotal =
                Pools.byteArrayPool.totalSize + Pools.intArrayPool.totalSize + Pools.floatArrayPool.totalSize +
                        AudioPools.FAPool.totalSize + AudioPools.SAPool.totalSize
            val native = ByteBufferPool.getNativeAllocated()
            LOGGER.info(
                "" +
                        "JVM:\n" +
                        "  Used:   ${formatMiB(jvmUsed)} MiB\n" +
                        "  Free:   ${formatMiB(runtime.freeMemory())} MiB\n" +
                        "  Total:  ${formatMiB(total)} MiB\n" +
                        "JVM-Buffers:\n" +
                        "  ByteBuffer: ${formatMiB(Pools.byteBufferPool.totalSize)} MiB\n" +
                        "  Byte[]:     ${formatMiB(Pools.byteArrayPool.totalSize)} MiB\n" +
                        "  Short[]:    ${formatMiB(AudioPools.SAPool.totalSize)} MiB\n" +
                        "  Int[]:      ${formatMiB(Pools.intArrayPool.totalSize)} MiB\n" +
                        "  Float[]:    ${formatMiB(Pools.floatArrayPool.totalSize)} MiB + ${formatMiB(AudioPools.FAPool.totalSize)} MiB (General + Audio)\n" +
                        "  Total:      ${formatMiB(jvmBufferTotal)} MiB\n" +
                        "VRAM:\n" +
                        "  Texture2d:      ${formatMiB(Texture2D.allocated.get())} MiB\n" +
                        "  Texture3d:      ${formatMiB(Texture3D.allocated.get())} MiB\n" +
                        "  Texture2dArray: ${formatMiB(Texture2DArray.allocated.get())} MiB\n" +
                        "  TextureCubemap: ${formatMiB(CubemapTexture.allocated.get())} MiB\n" +
                        "  Geometry:       ${formatMiB(GPUBuffer.allocated.get())} MiB\n" +
                        "  Total:          ${formatMiB(vramTotal)} MiB\n" +
                        "Native: ${formatMiB(native)} MiB\n" +
                        "Total:  ${formatMiB(native + vramTotal + jvmBufferTotal + jvmUsed)} MiB"
            )
        }

    }
}