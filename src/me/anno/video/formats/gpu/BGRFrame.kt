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

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val data = input.readNBytes2(s0 * 3, Texture2D.bufferPool)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("BGR", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                rgb.createBGR(data, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }
}