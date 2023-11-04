package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.shader2DYUV
import me.anno.gpu.shader.ShaderLib.shader3DYUV
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.LOGGER
import me.anno.utils.Sleep
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.InputStream

// this seems to work, and to be correct
class I444Frame(iw: Int, ih: Int) : GPUFrame(iw, ih, 3, 2) {

    private val y = Texture2D("i444-y-frame", width, height, 1)
    private val uv = Texture2D("i444-uv-frame", width, height, 1)

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val yData = input.readNBytes2(s0, Texture2D.bufferPool)
        blankDetector.putChannel(yData, 0)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("I444-Y", width, height) {
            if (!isDestroyed && !y.isDestroyed) {
                y.createMonochrome(yData, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }

        val uData = input.readNBytes2(s0, Texture2D.bufferPool)
        blankDetector.putChannel(uData, 1)
        val vData = input.readNBytes2(s0, Texture2D.bufferPool)
        blankDetector.putChannel(vData, 2)
        // merge the u and v planes
        val interlaced = interlaceReplace(uData, vData)
        // create the uv texture
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("I444-UV", width, height) {
            if (!isDestroyed && !uv.isDestroyed) {
                uv.createRG(interlaced, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }

    override fun getTextures(): List<Texture2D> = listOf(y, uv)

    override fun bind(offset: Int, nearestFiltering: GPUFiltering, clamping: Clamping) {
        uv.bind(offset + 1, nearestFiltering, clamping)
        y.bind(offset, nearestFiltering, clamping)
    }

    override fun bindUVCorrection(shader: Shader) {
        // all buffers are the same size, no correction required
        shader.v2f("uvCorrection", 1f, 1f)
    }

    override fun get2DShader() = shader2DYUV
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