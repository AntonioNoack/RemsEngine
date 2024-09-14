package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import me.anno.utils.pooling.Pools
import java.io.InputStream

class ARGBFrame(w: Int, h: Int) : RGBFrame(w, h, 4) {
    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height * 4
        val data = input.readNBytes2(s0, Pools.byteBufferPool)

        // change from argb to rgba
        for (i in 0 until s0 step 4) {
            val a = data[i]
            val r = data[i + 1]
            val g = data[i + 2]
            val b = data[i + 3]
            data.put(i, r)
            data.put(i + 1, g)
            data.put(i + 2, b)
            data.put(i + 3, a)
        }

        blankDetector.putRGBA(data)
        Sleep.acquire(true, creationLimiter) {
            addGPUTask("RGBA", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGBA(data, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }
}