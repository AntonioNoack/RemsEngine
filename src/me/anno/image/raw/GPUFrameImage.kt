package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager

/**
 * image for a video frame on the GPU
 * */
class GPUFrameImage(val frame: GPUFrame, numChannels: Int, hasAlphaChannel: Boolean) :
    Image(frame.width, frame.height, numChannels, hasAlphaChannel) {

    companion object {
        private val LOGGER = LogManager.getLogger(GPUFrameImage::class)
    }

    constructor(frame: GPUFrame, numChannels: Int) : this(frame, numChannels, numChannels > 3)
    constructor(frame: GPUFrame) : this(frame, frame.numChannels)

    override fun getRGB(index: Int): Int {
        val msg = "GPUFrameImage.getRGB() is extremely inefficient!!!"
        LOGGER.warn(msg, RuntimeException(msg))
        val image = asIntImage()
        val color = image.getRGB(index)
        image.destroy()
        return color
    }

    override fun asIntImage(): IntImage {
        val texture = frame.toTexture()
        val image = texture.createImage(false, hasAlphaChannel).asIntImage()
        texture.destroy()
        return image
    }

    override fun write(dst: FileReference, quality: Float) {
        asIntImage().write(dst, quality)
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        if (GFX.isGFXThread()) {
            frame.toTexture(texture)
            callback.ok(texture)
        } else addGPUTask("frame.toTexture", width, height) {
            createTexture(texture, sync, checkRedundancy, callback)
        }
    }

    override fun toString(): String {
        return "GPUFrameImage { $frame, $numChannels ch, ${if (hasAlphaChannel) "alpha" else "opaque"} }"
    }
}