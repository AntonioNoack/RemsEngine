package me.anno.image.raw

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.black
import me.anno.utils.async.Callback
import me.anno.utils.pooling.Pools.byteBufferPool
import org.joml.Vector4f

open class GrayscaleImage(val src: Image) : Image(
    src.width, src.height, 1,
    false, src.offset, src.stride
) {

    override fun getRGB(index: Int): Int = (getLuminance(src.getRGB(index)) * 0x10101) or black

    override fun setRGB(index: Int, value: Int) {
        src.setRGB(index, value)
    }

    override fun setRGB(index: Int, value: Vector4f) {
        src.setRGB(index, value)
    }

    override fun createTextureImpl(
        texture: Texture2D, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) = createTexture(texture, checkRedundancy, src, callback)

    private fun createTexture(
        texture: Texture2D, checkRedundancy: Boolean, src: Image,
        callback: Callback<ITexture2D>
    ) {
        val size = width * height
        if (src.numChannels == 1) {
            src.createTexture(texture, checkRedundancy, callback)
        } else when (src) {
            is GPUImage -> TextureMapper.mapTexture(src.texture, texture, "lll1", TargetType.UInt8x1, callback)
            is ComponentImage, is OpaqueImage -> src.createTextureImpl(texture, checkRedundancy, callback)
            else -> {
                val bytes = byteBufferPool[size, false, false]
                for (y in 0 until src.height) {
                    val flippedY = src.height - 1 - y
                    for (x in 0 until src.width) {
                        val color = getLuminance(src.getRGB(x, y)).toByte()
                        bytes.put(x + flippedY * src.width, color)
                    }
                }
                texture.createMonochrome(bytes, checkRedundancy)
                callback.ok(texture)
            }
        }
    }

    companion object {

        fun getLuminance(r: Byte, g: Byte, b: Byte): Int {
            return getLuminance(r.toInt().and(255), g.toInt().and(255), b.toInt().and(255))
        }

        fun getLuminance(rgb: Int): Int {
            val r = rgb.shr(16).and(255)
            val g = rgb.shr(8).and(255)
            val b = rgb.and(255)
            return getLuminance(r, g, b)
        }

        fun getLuminance(r: Int, g: Int, b: Int): Int {
            // 0.2126*r + 0.7152*g + 0.0722*b
            // *2^16, >>16
            return (13941 * r + 46873 * g + 4735 * b) shr 16
        }
    }
}