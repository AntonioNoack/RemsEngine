package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import java.io.InputStream

class BGRAFrame(w: Int, h: Int) : RGBFrame(w, h, 4) {

    override fun load(input: InputStream) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 4, Texture2D.bufferPool)
        blankDetector.putRGBA(data)
        Sleep.acquire(true, creationLimiter) {
            GFX.addGPUTask("BGRA", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGBA(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }

    override fun getShaderStage() = swizzleStageBGRA
}
