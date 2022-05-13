package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib.shader3DARGB
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep.acquire
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

class ARGBFrame(w: Int, h: Int) : GPUFrame(w, h, 0) {

    private val argb = Texture2D("argb-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h * 4
        val data = input.readNBytes2(s0, Texture2D.bufferPool)
        blankDetector.putRGBA(data)
        acquire(true, creationLimiter)
        GFX.addGPUTask(w, h) {
            // the data actually still is argb and shuffling is needed
            // to convert it into rgba (needs to be done in the shader (or by a small preprocessing step of the data))
            argb.createRGBA(data, true)
            creationLimiter.release()
        }
    }

    override fun get3DShader() = shader3DARGB
    override fun getTextures(): List<Texture2D> = listOf(argb)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        argb.bind(offset, nearestFiltering, clamping)
    }

}