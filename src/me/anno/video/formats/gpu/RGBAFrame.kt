package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import java.io.EOFException
import java.io.InputStream

class RGBAFrame(w: Int, h: Int) : RGBFrame(w, h) {
    override fun load(input: InputStream) {
        val s0 = width * height
        val data = Texture2D.bufferPool[s0 * 4, false, false]
        data.position(0)
        var hasAlpha = false
        for (i in 0 until s0) {
            val r = input.read()
            val g = input.read()
            val b = input.read()
            val a = input.read()
            if (r < 0 || g < 0 || b < 0 || a < 0) {
                Texture2D.bufferPool.returnBuffer(data)
                throw EOFException()
            }
            data.put(r.toByte())
            data.put(g.toByte())
            data.put(b.toByte())
            data.put(a.toByte()) // offset is required
            hasAlpha = hasAlpha || a < 255
        }
        numChannels = if (hasAlpha) 4 else 3
        data.flip()
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("RGBA", width, height) {
            rgb.createRGBA(data, true)
            creationLimiter.release()
        }
    }
}