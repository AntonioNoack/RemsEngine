package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.shader3DYUV
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.utils.input.Input.readNBytes2
import java.io.InputStream

class I420Frame(iw: Int, ih: Int) : GPUFrame(iw, ih, 2) {

    // this is correct, confirmed by example
    private val w2 get() = (w + 1) / 2
    private val h2 get() = (h + 1) / 2

    private val y = Texture2D("i420-y-frame", w, h, 1)
    private val uv = Texture2D("i420-uv-frame", w2, h2, 1)

    override fun load(input: InputStream) {
        val s0 = w * h
        val s1 = w2 * h2
        val yData = input.readNBytes2(s0, bufferPool)
        // writeMonochromeDebugImage(w, h, yData)
        creationLimiter.acquire()
        GFX.addGPUTask(w, h) {
            y.createMonochrome(yData, true)
            creationLimiter.release()
        }
        // merge the u and v planes
        val uData = input.readNBytes2(s1, bufferPool)
        val vData = input.readNBytes2(s1, bufferPool)
        val interlaced = interlace(uData, vData, bufferPool[s1 * 2, false])
        bufferPool.returnBuffer(uData)
        bufferPool.returnBuffer(vData)
        // create the uv texture
        creationLimiter.acquire()
        GFX.addGPUTask(w2, h2) {
            uv.createRG(interlaced, true)
            creationLimiter.release()
        }
    }

    override fun get3DShader() = shader3DYUV

    override fun getTextures(): List<Texture2D> = listOf(y, uv)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        uv.bind(offset + 1, nearestFiltering, clamping)
        y.bind(offset, nearestFiltering, clamping)
    }

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