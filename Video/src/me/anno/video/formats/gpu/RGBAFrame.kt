package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import java.io.EOFException
import java.io.InputStream

class RGBAFrame(w: Int, h: Int) : RGBFrame(w, h, 4) {
    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val data = Texture2D.bufferPool[s0 * 4, false, false]
        data.position(0)
        for (i in 0 until s0) {
            val r = input.read()
            val g = input.read()
            val b = input.read()
            val a = input.read()
            if (a < 0) {
                Texture2D.bufferPool.returnBuffer(data)
                throw EOFException()
            }
            data.put(r.toByte())
            data.put(g.toByte())
            data.put(b.toByte())
            data.put(a.toByte()) // offset is required
        }
        data.flip()
        Sleep.acquire(true, creationLimiter) {
            GFX.addGPUTask("RGBA", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGBA(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }
}