package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class ARGBFrame(w: Int, h: Int) : GPUFrame(w, h, 4, 0) {

    companion object {
        private val LOGGER = LogManager.getLogger(ARGBFrame::class)
    }

    private val rgba = Texture2D("rgba", w, h, 1)

    override fun getByteSize(): Long {
        return width * height * 4L
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height * 4
        val data = input.readNBytes2(s0, Texture2D.bufferPool)

        // change from argb to rgba
        var hasAlpha = false
        for (i in 0 until s0 step 4) {
            val a = data[i]
            val r = data[i + 1]
            val g = data[i + 2]
            val b = data[i + 3]
            data.put(i, r)
            data.put(i + 1, g)
            data.put(i + 2, b)
            data.put(i + 3, a)
            hasAlpha = hasAlpha || a.toInt() != -1
        }
        numChannels = if (hasAlpha) 4 else 3

        blankDetector.putRGBA(data)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("RGBA", width, height) {
            if (!isDestroyed && !rgba.isDestroyed) {
                rgba.createRGBA(data, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }

    override fun getShaderStage() = swizzleStages[""]
    override fun getTextures(): List<Texture2D> = listOf(rgba)

    override fun bind(offset: Int, nearestFiltering: Filtering, clamping: Clamping) {
        rgba.bind(offset, nearestFiltering, clamping)
    }
}