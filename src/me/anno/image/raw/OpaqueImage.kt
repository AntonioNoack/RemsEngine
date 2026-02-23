package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.black
import me.anno.utils.async.Callback
import org.joml.Vector4f
import kotlin.math.min

/**
 * turns any image into an image without alpha channel
 * */
open class OpaqueImage(val src: Image) : Image(
    src.width, src.height, min(3, src.numChannels),
    false, src.offset, src.stride
) {

    override fun getRGB(index: Int): Int = src.getRGB(index) or black

    override fun setRGB(index: Int, value: Int) {
        src.setRGB(index, value)
    }

    override fun setRGB(index: Int, value: Vector4f) {
        src.setRGB(index, value)
    }

    override fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        when {
            !src.hasAlphaChannel -> src.createTextureImpl(texture, checkRedundancy, callback)
            src is GPUImage -> TextureMapper.mapTexture(src.texture, texture, "rgb1", TargetType.UInt8x4, callback)
            src is FloatImage -> {
                val withoutAlpha = FloatImage(width, height, numChannels - 1)
                withoutAlpha.forEachPixel { x, y ->
                    for (c in 0 until withoutAlpha.numChannels) {
                        withoutAlpha.setValue(x, y, c, src.getValue(x, y, c))
                    }
                }
                withoutAlpha.createTextureImpl(texture, checkRedundancy, callback)
            }
            else -> {
                IntImage(width, height, src.asIntImage().data, false, offset, stride)
                    .createTexture(texture, checkRedundancy, callback)
            }
        }
    }
}