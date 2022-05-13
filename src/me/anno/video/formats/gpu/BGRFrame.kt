package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {

    override fun load(input: InputStream) {
        val s0 = w * h
        val data = input.readNBytes2(s0 * 3, Texture2D.bufferPool)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask(w, h) {
            rgb.createBGR(data, true)
            creationLimiter.release()
        }
    }

}