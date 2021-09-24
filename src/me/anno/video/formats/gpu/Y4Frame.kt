package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.input.Input.readNBytes2
import java.io.InputStream

class Y4Frame(w: Int, h: Int) : RGBFrame(w, h) {

    override fun load(input: InputStream) {
        val s0 = w * h
        val data = input.readNBytes2(s0, Texture2D.bufferPool)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            rgb.createMonochrome(data, true)
            creationLimiter.release()
        }
    }

}