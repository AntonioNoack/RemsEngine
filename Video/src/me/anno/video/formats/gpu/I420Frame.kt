package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.Sleep
import me.anno.video.formats.gpu.I444Frame.Companion.yuvStage
import java.io.InputStream

class I420Frame(iw: Int, ih: Int) : GPUFrame(iw, ih, 3) {

    private val w2 get() = (width + 1) ushr 1
    private val h2 get() = (height + 1) ushr 1

    private val s0 get() = width * height
    private val s1 get() = w2 * h2

    private val y = Texture2D("i420.y", width, height, 1)
    private val uv = Texture2D("i420.uv", w2, h2, 1)

    override fun getByteSize(): Long {
        return (width * height) + 2L * (w2 * h2)
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val yData = input.readNBytes2(s0, Texture2D.bufferPool)
        blankDetector.putChannel(yData, 0)
        val uData = input.readNBytes2(s1, Texture2D.bufferPool)
        blankDetector.putChannel(uData, 1)
        val vData = input.readNBytes2(s1, Texture2D.bufferPool)
        blankDetector.putChannel(vData, 2)
        val interlaced = interlaceReplace(uData, vData)
        Sleep.acquire(true, creationLimiter) {
            addGPUTask("I420-UV", width, height) {
                if (!isDestroyed && !y.isDestroyed && !uv.isDestroyed) {
                    y.createMonochrome(yData, false)
                    uv.createRG(interlaced, false)
                } else warnAlreadyDestroyed()
                creationLimiter.release()
            }
        }
    }

    override fun getShaderStage() = yuvStage
    override fun getTextures(): List<Texture2D> = listOf(y, uv)
}