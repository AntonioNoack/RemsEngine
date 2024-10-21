package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.black
import me.anno.utils.async.Callback
import kotlin.math.min

/**
 * turns any image into an image without alpha channel
 * */
open class OpaqueImage(val src: Image) :
    Image(src.width, src.height, min(3, src.numChannels), false, src.offset, src.stride) {

    override fun getRGB(index: Int): Int = src.getRGB(index) or black

    override fun createTexture(
        texture: Texture2D, sync: Boolean,
        checkRedundancy: Boolean, callback: Callback<ITexture2D>
    ) {
        when {
            !src.hasAlphaChannel -> {
                src.createTexture(texture, sync, checkRedundancy, callback)
            }
            src is GPUImage -> {
                TextureMapper.mapTexture(src.texture, texture, "rgb1", TargetType.UInt8x4, callback)
            }
            else -> {
                val opaque = IntImage(width, height, src.asIntImage().data, false, offset, stride)
                opaque.createTexture(texture, sync, checkRedundancy, callback)
            }
        }
    }
}