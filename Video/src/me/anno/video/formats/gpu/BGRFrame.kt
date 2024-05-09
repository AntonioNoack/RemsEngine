package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {
    override fun load(input: InputStream) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 3, Texture2D.bufferPool)
        Sleep.acquire(true, creationLimiter) {
            GFX.addGPUTask("BGR", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createBGR(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }
}