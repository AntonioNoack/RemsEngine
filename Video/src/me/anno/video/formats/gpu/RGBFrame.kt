package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import java.io.EOFException
import java.io.InputStream

open class RGBFrame(w: Int, h: Int, numChannels: Int, code: Int) : GPUFrame(w, h, numChannels, code) {

    constructor(w: Int, h: Int) : this(w, h, 3, -1)

    val rgb = Texture2D("rgb-frame", width, height, 1)

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
        Sleep.acquire(true, creationLimiter) {
            GFX.addGPUTask("RGB", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGB(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }

    override fun getTextures(): List<Texture2D> = listOf(rgb)
}