package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.io.Streams.readNBytes2
import me.anno.utils.pooling.Pools
import java.io.InputStream

class BGRAFrame(w: Int, h: Int) : RGBFrame(w, h, 4) {

    override fun load(input: InputStream, callback: (GPUFrame?) -> Unit) {
        if (isDestroyed) return
        val data = input.readNBytes2(width * height * 4, Pools.byteBufferPool)
        blankDetector.putRGBA(data)
        addGPUTask("BGRA", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                // RGBA is fine here, because it's swizzled in the shader
                rgb.createRGBA(data, false)
            } else warnAlreadyDestroyed(data, null)
            callback(this)
        }
    }

    override fun getShaderStage() = swizzleStageBGRA
}
