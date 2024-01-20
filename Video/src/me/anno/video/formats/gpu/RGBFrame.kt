package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.InputStream

open class RGBFrame(w: Int, h: Int) : GPUFrame(w, h, 3, -1) {

    companion object {
        private val LOGGER = LogManager.getLogger(RGBFrame::class)
    }

    val rgb = Texture2D("rgb-frame", w, h, 1)

    override fun getByteSize(): Long {
        return width * height * 3L
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val data = Texture2D.bufferPool[s0 * 4, false, false]
        data.position(0)
        for (i in 0 until s0) {
            val r = input.read()
            val g = input.read()
            val b = input.read()
            if (r < 0 || g < 0 || b < 0) {
                Texture2D.bufferPool.returnBuffer(data)
                throw EOFException()
            }
            data.put(r.toByte())
            data.put(g.toByte())
            data.put(b.toByte())
            data.put(-1) // offset is required
        }
        data.flip()
        blankDetector.putRGBA(data)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("RGB", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                rgb.createRGB(data, false)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }

    override fun getShaderStage() = swizzleStages[""]
    override fun getTextures(): List<Texture2D> = listOf(rgb)

    override fun bind(offset: Int, nearestFiltering: Filtering, clamping: Clamping) {
        rgb.bind(offset, nearestFiltering, clamping)
    }
}