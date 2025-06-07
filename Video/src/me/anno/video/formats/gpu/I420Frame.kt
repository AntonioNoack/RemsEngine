package me.anno.video.formats.gpu

import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D
import me.anno.io.Streams.readNBytes2
import me.anno.utils.pooling.Pools
import me.anno.video.formats.gpu.I444Frame.Companion.yuvStage
import java.io.InputStream

class I420Frame(iw: Int, ih: Int) : GPUFrame(iw, ih, 3) {

    private val halfWidth get() = (width + 1) ushr 1
    private val halfHeight get() = (height + 1) ushr 1

    private val imageSize get() = width * height
    private val halfImageSize get() = halfWidth * halfHeight

    private val y = Texture2D("i420.y", width, height, 1)
    private val uv = Texture2D("i420.uv", halfWidth, halfHeight, 1)

    override fun getByteSize(): Long {
        return imageSize + 2L * halfImageSize
    }

    override fun load(input: InputStream, callback: (GPUFrame?) -> Unit) {
        if (isDestroyed) return

        val yData = input.readNBytes2(imageSize, Pools.byteBufferPool)
        blankDetector.putChannel(yData, 0)
        val uData = input.readNBytes2(halfImageSize, Pools.byteBufferPool)
        blankDetector.putChannel(uData, 1)
        val vData = input.readNBytes2(halfImageSize, Pools.byteBufferPool)
        blankDetector.putChannel(vData, 2)
        val interlaced = interlaceReplace(uData, vData)
        addGPUTask("I420-UV", width, height) {
            if (!isDestroyed && !y.isDestroyed && !uv.isDestroyed) {
                y.createMonochrome(yData, false)
                uv.createRG(interlaced, false)
            } else warnAlreadyDestroyed(yData, interlaced)
            callback(this)
        }
    }

    override fun getShaderStage() = yuvStage
    override fun getTextures(): List<Texture2D> = listOf(y, uv)
}