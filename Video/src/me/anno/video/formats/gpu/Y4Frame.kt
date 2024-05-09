package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import java.io.InputStream

class Y4Frame(w: Int, h: Int) : GPUFrame(w, h, 1, -1) {

    val y = Texture2D("y4", width, height, 1)

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val data = input.readNBytes2(s0, Texture2D.bufferPool)
        Sleep.acquire(true, creationLimiter) {
            GFX.addGPUTask("Y4", width, height) {
                if (!isDestroyed && !y.isDestroyed) {
                    y.createMonochrome(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }

    override fun getShaderStage() = swizzleStageMono
    override fun getTextures(): List<Texture2D> = listOf(y)
}