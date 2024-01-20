package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {

    companion object {
        private val LOGGER = LogManager.getLogger(BGRFrame::class)
    }

    override fun getByteSize(): Long {
        return width * height * 3L
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 3, Texture2D.bufferPool)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("BGR", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                rgb.createBGR(data, false)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }
}