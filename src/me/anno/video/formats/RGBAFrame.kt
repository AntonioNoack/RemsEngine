package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import java.io.EOFException
import java.io.InputStream

class RGBAFrame(w: Int, h: Int) : RGBFrame(w, h) {

    override fun load(input: InputStream) {
        val s0 = w * h
        val data = Texture2D.byteBufferPool[s0 * 4, false]
        data.position(0)
        for (i in 0 until s0) {
            // todo is this correct?
            val r = input.read()
            val g = input.read()
            val b = input.read()
            val a = input.read()
            if (r < 0 || g < 0 || b < 0 || a < 0) throw EOFException()
            data.put(a.toByte())
            data.put(g.toByte())
            data.put(b.toByte())
            data.put(a.toByte()) // offset is required
        }
        data.position(0)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            rgb.createRGBA(data, true)
            creationLimiter.release()
        }
    }

}