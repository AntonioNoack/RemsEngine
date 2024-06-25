package me.anno.image.raw

import me.anno.utils.structures.Callback
import me.anno.gpu.framebuffer.TargetType.Companion.UInt8xI
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import org.apache.logging.log4j.LogManager

/**
 * image for a texture
 * */
class GPUImage(val texture: ITexture2D, numChannels: Int, hasAlphaChannel: Boolean) :
    Image(texture.width, texture.height, numChannels, hasAlphaChannel) {

    companion object {
        private val LOGGER = LogManager.getLogger(GPUImage::class)
    }

    constructor(texture: ITexture2D, numChannels: Int) : this(texture, numChannels, numChannels > 3)
    constructor(texture: ITexture2D) : this(texture, texture.channels)

    override fun getRGB(index: Int): Int {
        return when (texture) {
            TextureLib.invisibleTexture -> 0
            TextureLib.whiteTexture -> white
            TextureLib.blackTexture -> black
            TextureLib.missingTexture -> TextureLib.missingColors[index]
            else -> {
                val msg = "GPUImage.getRGB() is highly inefficient!!!"
                LOGGER.warn(msg, RuntimeException(msg))
                // is not flipping correct?
                (texture as Texture2D)
                    .createImage(false, hasAlphaChannel)
                    .getRGB(index)
            }
        }
    }

    override fun write(dst: FileReference, quality: Float) {
        texture.createImage(false, hasAlphaChannel).write(dst, quality)
    }

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        val mapping = when (numChannels) {
            1 -> "r111"
            2 -> "rg11"
            3 -> "rgb1"
            else -> "rgba"
        }
        val type = UInt8xI[numChannels - 1]
        TextureMapper.mapTexture(this.texture, texture, mapping, type, callback)
    }

    override fun asIntImage() = texture.createImage(false, hasAlphaChannel)

    override fun toString(): String {
        return "GPUImage { $texture, $numChannels ch, ${if (hasAlphaChannel) "alpha" else "opaque"} }"
    }
}