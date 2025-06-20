package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.pooling.Pools
import java.io.InputStream

class Y4Frame(w: Int, h: Int) : GPUFrame(w, h, 1) {

    val y = Texture2D("y4", width, height, 1)

    override fun load(input: InputStream, callback: (GPUFrame?) -> Unit) {
        if (isDestroyed) return

        val data = input.readNBytes2(width * height, Pools.byteBufferPool)
        blankDetector.putR(data)
        addGPUTask("Y4", width, height) {
            if (!isDestroyed && !y.isDestroyed) {
                y.createMonochrome(data, false)
            } else warnAlreadyDestroyed(data, null)
            callback(this)
        }
    }

    override fun getShaderStage() = swizzleStageMono
    override fun getTextures(): List<Texture2D> = listOf(y)
}