package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.shader.ShaderLib.shader2DBGRA
import me.anno.gpu.shader.ShaderLib.shader3DBGRA
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep.acquire
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

class BGRAFrame(w: Int, h: Int) : GPUFrame(w, h, 4, 1) {

    private val bgra = Texture2D("bgra-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = width * height * 4
        val data = input.readNBytes2(s0, Texture2D.bufferPool)
        // check whether alpha is actually present, and save that to numChannels
        val hasAlpha = (0 until s0 step 4).any {
            data[it + 3].toInt() != -1
        }
        numChannels = if (hasAlpha) 4 else 3
        blankDetector.putRGBA(data)
        acquire(true, creationLimiter)
        GFX.addGPUTask("BGRA", width, height) {
            bgra.createRGBA(data, true)
            Texture2D.bufferPool.returnBuffer(data)
            creationLimiter.release()
        }
    }

    override fun get2DShader() = shader2DBGRA
    override fun get3DShader() = shader3DBGRA
    override fun getTextures(): List<Texture2D> = listOf(bgra)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        bgra.bind(offset, nearestFiltering, clamping)
    }
}