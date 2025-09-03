package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D.Companion.convertRGB2BGR
import me.anno.io.Streams.readNBytes2
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools
import java.io.IOException
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {
    override fun load(input: InputStream, callback: Callback<GPUFrame>) {
        if (isDestroyed) return callback.err(IOException("Already destroyed"))

        try {
            val data = input.readNBytes2(width * height * 3, Pools.byteBufferPool)
            blankDetector.putRGB(data)
            convertRGB2BGR(data)

            addGPUTask("BGR", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGB(data, false)
                } else warnAlreadyDestroyed(data, null)
                callback.ok(this)
            }
        } catch (e: Exception) {
            callback.err(e)
        }
    }
}