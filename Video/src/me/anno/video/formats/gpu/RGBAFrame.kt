package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.utils.Sleep
import me.anno.utils.pooling.Pools
import java.io.EOFException
import java.io.InputStream

class RGBAFrame(width: Int, height: Int) : RGBFrame(width, height, 4) {
    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val data = Pools.byteBufferPool[s0 * 4, false, false]
        data.position(0)
        repeat(s0) {
            val r = input.read()
            val g = input.read()
            val b = input.read()
            val a = input.read()
            if (a < 0) {
                Pools.byteBufferPool.returnBuffer(data)
                throw EOFException()
            }
            data.put(r.toByte())
            data.put(g.toByte())
            data.put(b.toByte())
            data.put(a.toByte()) // offset is required
        }
        data.flip()

        Sleep.acquire(true, creationLimiter) {
            addGPUTask("RGBA", width, height) {
                if (!isDestroyed && !rgb.isDestroyed) {
                    rgb.createRGBA(data, false)
                } else warnAlreadyDestroyed(data, null)
                creationLimiter.release()
            }
        }
    }
}