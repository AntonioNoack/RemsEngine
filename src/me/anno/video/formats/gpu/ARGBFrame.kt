package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib.shader2DRGBA
import me.anno.gpu.shader.ShaderLib.shader3DRGBA
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep.acquire
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

class ARGBFrame(w: Int, h: Int) : GPUFrame(w, h, 0) {

    private val rgba = Texture2D("rgba", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h * 4
        val data = input.readNBytes2(s0, Texture2D.bufferPool)

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
        acquire(true, creationLimiter)
        GFX.addGPUTask("RGBA", w, h) {
            rgba.createRGBA(data, true)
            creationLimiter.release()
        }
    }

    override fun get2DShader() = shader2DRGBA
    override fun get3DShader() = shader3DRGBA
    override fun getTextures(): List<Texture2D> = listOf(rgba)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        rgba.bind(offset, nearestFiltering, clamping)
    }

}