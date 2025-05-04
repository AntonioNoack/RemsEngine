package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.framebuffer.TargetType.Companion.UInt8xI
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.IndestructibleTexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager
import kotlin.math.max

/**
 * image for a texture
 * */
class GPUImage(val texture: ITexture2D, numChannels: Int, hasAlphaChannel: Boolean) :
    Image(texture.width, texture.height, numChannels, hasAlphaChannel) {

    companion object {
        private val LOGGER = LogManager.getLogger(GPUImage::class)
        private val blankImage = IntImage(1, 1, false)
    }

    constructor(texture: ITexture2D, numChannels: Int) : this(texture, numChannels, numChannels > 3)
    constructor(texture: ITexture2D) : this(texture, max(texture.channels, 1))

    override fun getRGB(index: Int): Int {
        return if (texture is IndestructibleTexture2D) {
            texture.getRGB(index)
        } else {
            val msg = "GPUImage.getRGB(${texture.javaClass.simpleName}) is very inefficient!!!"
            LOGGER.warn(msg, RuntimeException(msg))
            // is not flipping correct?
            (texture as Texture2D)
                .createImage(false, hasAlphaChannel)
                .getRGB(index)
        }
    }

    override fun setRGB(index: Int, value: Int) {
        LOGGER.warn("Setting pixels in GPUImage not yet supported")
    }

    override fun write(dst: FileReference, quality: Float) {
        texture.createImage(false, hasAlphaChannel).write(dst, quality)
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        val mapping = when (numChannels) {
            1 -> "r111"
            2 -> "rg11"
            3 -> "rgb1"
            else -> "rgba"
        }
        val type = UInt8xI[numChannels - 1]
        TextureMapper.mapTexture(this.texture, texture, mapping, type, callback)
    }

    override fun asIntImage(): IntImage {
        return if (GFX.isGFXThread()) {
            if (texture.isCreated()) {
                texture.createImage(false, hasAlphaChannel).asIntImage()
            } else {
                val reason = if (texture.isDestroyed) "it was destroyed" else "it wasn't created"
                LOGGER.warn("Failed converting '${texture.name}' to image, because $reason")
                return IntImage(1, 1, false)
            }
        } else {
            LOGGER.warn("Waiting on GFXThread to convert '${texture.name}' to image")
            var image: IntImage? = null
            addGPUTask("GPUImage->Texture", width, height) {
                image = asIntImage()
            }
            waitUntilDefined(true) { image }
                ?: blankImage
        }
    }

    override fun resized(dstWidth: Int, dstHeight: Int, allowUpscaling: Boolean): Image {
        // otherwise, this would use getRGB a thousand times, which is very slow
        if (width == dstWidth && height == dstHeight) return this
        return asIntImage().resized(dstWidth, dstHeight, allowUpscaling)
    }

    override fun toString(): String {
        return "GPUImage { $texture, $numChannels ch, ${if (hasAlphaChannel) "alpha" else "opaque"} }"
    }
}