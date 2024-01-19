package me.anno.video.formats.gpu

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Sleep
import me.anno.utils.types.InputStreams.readNBytes2
import me.anno.video.formats.gpu.I444Frame.Companion.yuvStage
import org.apache.logging.log4j.LogManager
import java.io.InputStream

class I420Frame(iw: Int, ih: Int) : GPUFrame(iw, ih, 3, 2) {

    companion object {
        private val LOGGER = LogManager.getLogger(I420Frame::class)
    }

    // this is correct, confirmed by example
    private val w2 get() = (width + 1) / 2
    private val h2 get() = (height + 1) / 2

    private val y = Texture2D("i420-y-frame", width, height, 1)
    private val uv = Texture2D("i420-uv-frame", w2, h2, 1)

    override fun getByteSize(): Long {
        return (width * height) + 2L * (w2 * h2)
    }

    override fun load(input: InputStream) {
        if (isDestroyed) return

        val s0 = width * height
        val s1 = w2 * h2
        // load and create y plane
        val yData = input.readNBytes2(s0, Texture2D.bufferPool)
        blankDetector.putChannel(yData, 0)
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("I420-Y", width, height) {
            if (!isDestroyed && !y.isDestroyed) {
                y.createMonochrome(yData, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
        // merge and create u/v planes
        val uData = input.readNBytes2(s1, Texture2D.bufferPool)
        blankDetector.putChannel(uData, 1)
        val vData = input.readNBytes2(s1, Texture2D.bufferPool)
        blankDetector.putChannel(vData, 2)
        // merge the u and v planes
        val interlaced = interlaceReplace(uData, vData)
        // create the uv texture
        Sleep.acquire(true, creationLimiter)
        GFX.addGPUTask("I420-UV", w2, h2) {
            if (!isDestroyed && !uv.isDestroyed) {
                uv.createRG(interlaced, true)
            } else LOGGER.warn(frameAlreadyDestroyed)
            creationLimiter.release()
        }
    }

    override fun getShaderStage() = yuvStage
    override fun getTextures(): List<Texture2D> = listOf(y, uv)

    override fun bind(offset: Int, nearestFiltering: Filtering, clamping: Clamping) {
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