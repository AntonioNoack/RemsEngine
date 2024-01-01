package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep.acquire
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class BGRAFrame(w: Int, h: Int) : GPUFrame(w, h, 4, 1) {

    companion object {
        private val LOGGER = LogManager.getLogger(BGRAFrame::class)
    }

    private val bgra = Texture2D("bgra-frame", w, h, 1)

    override fun getByteSize(): Long {
        return width * height * 4L
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height * 4
        val data = input.readNBytes2(s0, Texture2D.bufferPool)
        // check whether alpha is actually present, and save that to numChannels
        val hasAlpha = (0 until s0 step 4).any {
            data[it + 3].toInt() != -1
        }
        numChannels = if (hasAlpha) 4 else 3
        blankDetector.putRGBA(data)
        acquire(true, creationLimiter)
        GFX.addGPUTask("BGRA", width, height) {
            if (!isDestroyed && !bgra.isDestroyed) {
                bgra.createRGBA(data, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }

    override fun getShaderStage() = swizzleStages[".bgra"]
    override fun getTextures(): List<Texture2D> = listOf(bgra)

    override fun bind(offset: Int, nearestFiltering: Filtering, clamping: Clamping) {
        bgra.bind(offset, nearestFiltering, clamping)
    }
}