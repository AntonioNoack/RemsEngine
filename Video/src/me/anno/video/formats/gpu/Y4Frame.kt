package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import me.anno.io.Streams.readNBytes2
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class Y4Frame(w: Int, h: Int) : RGBFrame(w, h) {

    companion object {
        private val LOGGER = LogManager.getLogger(Y4Frame::class)
    }

    override fun getByteSize(): Long {
        return width * height.toLong()
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val data = input.readNBytes2(s0, Texture2D.bufferPool)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("Y4", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                rgb.createMonochrome(data, false)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }
}