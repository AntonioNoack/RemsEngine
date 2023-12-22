package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType.Companion.UByteTargets
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager

class GPUImage(val texture: ITexture2D, numChannels: Int, hasAlphaChannel: Boolean) :
    Image(texture.width, texture.height, numChannels, hasAlphaChannel) {

    companion object {
        private val LOGGER = LogManager.getLogger(GPUImage::class)
    }

    constructor(texture: ITexture2D, numChannels: Int) : this(texture, numChannels, numChannels > 3)
    constructor(texture: Texture2D) : this(texture, texture.numChannels)

    override fun getRGB(index: Int): Int {
        val msg = "GPUImage.getRGB() is highly inefficient!!!"
        LOGGER.warn(msg, RuntimeException(msg))
        // is not flipping correct?
        return (texture as Texture2D).createImage(false, hasAlphaChannel)
            .getRGB(index)
    }

    override fun createBufferedImage() =
        texture.createImage(false, hasAlphaChannel).createBufferedImage()

    override fun write(dst: FileReference) {
        texture.createImage(false, hasAlphaChannel).write(dst)
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: (Texture2D?, Exception?) -> Unit
    ) {
        val mapping = when (numChannels) {
            1 -> "r111"
            2 -> "rg11"
            3 -> "rgb1"
            else -> "rgba"
        }
        val type = UByteTargets[numChannels - 1]
        TextureMapper.mapTexture(this.texture, texture, mapping, type, callback)
    }

    override fun createIntImage() = texture.createImage(false, hasAlphaChannel)

    override fun toString(): String {
        return "GPUImage { $texture, $numChannels ch, ${if (hasAlphaChannel) "alpha" else "opaque"} }"
    }
}