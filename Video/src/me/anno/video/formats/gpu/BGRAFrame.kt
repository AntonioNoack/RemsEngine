package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import me.anno.utils.pooling.Pools
import java.io.InputStream

class BGRAFrame(w: Int, h: Int) : RGBFrame(w, h, 4) {

    override fun load(input: InputStream) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 4, Pools.byteBufferPool)
        blankDetector.putRGBA(data)
        Sleep.acquire(true, creationLimiter) {
            addGPUTask("BGRA", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGBA(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }

    override fun getShaderStage() = swizzleStageBGRA
}
