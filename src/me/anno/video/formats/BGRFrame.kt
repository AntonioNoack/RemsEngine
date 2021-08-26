package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.utils.input.readNBytes2
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {

    override fun load(input: InputStream) {
        val s0 = w * h
        val data = input.readNBytes2(s0 * 3, false)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            rgb.createRGB(data, true)
            creationLimiter.release()
        }
    }

}