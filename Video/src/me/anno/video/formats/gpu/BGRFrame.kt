package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.io.Streams.readNBytes2
import me.anno.utils.pooling.Pools
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {
    override fun load(input: InputStream, callback: (GPUFrame?) -> Unit) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 3, Pools.byteBufferPool)
        blankDetector.putRGB(data)
        addGPUTask("BGR", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                rgb.createBGR(data, false)
            } else warnAlreadyDestroyed(data, null)
            callback(this)
        }
    }
}