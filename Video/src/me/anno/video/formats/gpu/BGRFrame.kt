package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import me.anno.utils.pooling.Pools
import java.io.InputStream

class BGRFrame(w: Int, h: Int) : RGBFrame(w, h) {
    override fun load(input: InputStream) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 3, Pools.byteBufferPool)
        Sleep.acquire(true, creationLimiter) {
            addGPUTask("BGR", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createBGR(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }
}