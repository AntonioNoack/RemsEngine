package me.anno.video.formats

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DYUV
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.input.Input.readNBytes2
import me.anno.video.VFrame
import java.io.InputStream

// this seems to work, and to be correct
class I444Frame(iw: Int, ih: Int) : VFrame(iw, ih, 2) {

    private val y = Texture2D("i444-y-frame", w, h, 1)
    private val u = Texture2D("i444-u-frame", w, h, 1)
    private val v = Texture2D("i444-v-frame", w, h, 1)

    override fun load(input: InputStream) {
        val s0 = w * h
        val yData = input.readNBytes2(s0, Texture2D.bufferPool)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            y.createMonochrome(yData, true)
            creationLimiter.release()
        }
        val uData = input.readNBytes2(s0, Texture2D.bufferPool)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            u.createMonochrome(uData, true)
            creationLimiter.release()
        }
        val vData = input.readNBytes2(s0, Texture2D.bufferPool)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            v.createMonochrome(vData, true)
            creationLimiter.release()
            // tasks are executed in order, so this is true
            // (if no exception happened)
        }
    }

    override fun getTextures(): List<Texture2D> = listOf(y, u, v)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        v.bind(offset + 2, nearestFiltering, clamping)
        u.bind(offset + 1, nearestFiltering, clamping)
        y.bind(offset, nearestFiltering, clamping)
    }

    override fun bindUVCorrection(shader: Shader) {
        // all buffers are the same size, no correction required
        shader.v2("uvCorrection", 1f, 1f)
    }

    override fun get3DShader() = shader3DYUV

    // 319x yuv = 2,400 MB
    // 7.5 MB / yuv
    // 7.5 MB / 1.5 =
    // 5 MB / full channel
    // = 2.4x what is really needed...
    // 305x RGBA uv = 7,000 MB
    // 23 MB / RGBA uv
    // 5.1 MB / full channel
    // -> awkward....

}