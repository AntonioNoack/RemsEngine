package me.anno.image.raw

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager

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
        val image = createIntImage()
        val color = image.getRGB(index)
        image.destroy()
        return color
    }

    override fun createIntImage(): IntImage {
        val texture = frame.toTexture()
        val image = texture.createImage(false, hasAlphaChannel)
        texture.destroy()
        return image
    }

    override fun write(dst: FileReference) {
        createIntImage().write(dst)
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: (Texture2D?, Exception?) -> Unit
    ) {
        frame.toTexture(texture)
        callback(texture, null)
    }

    override fun toString(): String {
        return "GPUImage { $frame, $numChannels ch, ${if (hasAlphaChannel) "alpha" else "opaque"} }"
    }
}