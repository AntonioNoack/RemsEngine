package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools
import java.io.IOException
import java.io.InputStream

open class RGBFrame(w: Int, h: Int, numChannels: Int) : GPUFrame(w, h, numChannels) {

    constructor(w: Int, h: Int) : this(w, h, 3)

    val rgb = Texture2D("rgb-frame", width, height, 1)

    override fun load(input: InputStream, callback: Callback<GPUFrame>) {
        if (isDestroyed) return callback.err(IOException("Already destroyed"))

        try {
            val data = input.readNBytes2(width * height * 3, Pools.byteBufferPool)
                ?: return callback.err(null)

            blankDetector.putRGB(data)
            addGPUTask("RGB", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGB(data, false)
                } else warnAlreadyDestroyed(data, null)
                callback.ok(this)
            }
        } catch (e: Exception) {
            callback.err(e)
        }
    }

    override fun getTextures(): List<Texture2D> = listOf(rgb)
}