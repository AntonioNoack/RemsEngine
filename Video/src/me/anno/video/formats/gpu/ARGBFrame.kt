package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.io.Streams.readNBytes2
import me.anno.utils.pooling.Pools
import java.io.InputStream

class ARGBFrame(w: Int, h: Int) : RGBFrame(w, h, 4) {
    override fun load(input: InputStream, callback: (GPUFrame?) -> Unit) {
        if (isDestroyed) return

        val data = input.readNBytes2(width * height * 4, Pools.byteBufferPool)
        blankDetector.putARGB(data)
        addGPUTask("RGBA", width, height) {
            if (!isDestroyed && !rgb.isDestroyed) {
                rgb.createRGBA(data, false)
            } else warnAlreadyDestroyed(data, null)
            callback(this)
        }
    }

    override fun getShaderStage(): ShaderStage = swizzleStageARGB
}