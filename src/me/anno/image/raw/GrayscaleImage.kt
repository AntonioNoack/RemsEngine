package me.anno.image.raw

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2D.Companion.bufferPool
import me.anno.image.Image
import me.anno.utils.Color.black

open class GrayscaleImage(val src: Image) :
    Image(src.width, src.height, 1, false) {

    override fun getRGB(index: Int): Int = (getLuminance(src.getRGB(index)) * 0x10101) or black

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: (ITexture2D?, Exception?) -> Unit
    ) = createTexture(texture, sync, checkRedundancy, src, callback)

    private fun createTexture(
        texture: Texture2D, sync: Boolean,
        checkRedundancy: Boolean, src: Image,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        val size = width * height
        if (src.numChannels == 1) {
            src.createTexture(texture, sync, checkRedundancy, callback)
        } else when (src) {
            is IntImage -> {
                val data = src.data
                val bytes = bufferPool[size, false, false]
                for (i in 0 until size) {
                    bytes.put(i, getLuminance(data[i]).toByte())
                }
                if (sync && GFX.isGFXThread()) {
                    texture.createMonochrome(bytes, checkRedundancy)
                    callback(texture, null)
                } else {
                    if (checkRedundancy) texture.checkRedundancyMonochrome(bytes)
                    GFX.addGPUTask("GrayscaleImage.IntImage", width, height) {
                        texture.createMonochrome(bytes, checkRedundancy = false)
                        callback(texture, null)
                    }
                }
            }
            is ByteImage -> {
                val data = src.data
                val bytes = bufferPool[size, false, false]
                for (i in 0 until size) {
                    val j = i * 4
                    bytes.put(i, getLuminance(data[j + 1], data[j + 2], data[j + 3]).toByte())
                }
                if (sync && GFX.isGFXThread()) {
                    texture.createMonochrome(bytes, checkRedundancy)
                    callback(texture, null)
                } else {
                    if (checkRedundancy) texture.checkRedundancyMonochrome(bytes)
                    GFX.addGPUTask("GrayscaleImage.ByteImage", width, height) {
                        texture.createMonochrome(bytes, checkRedundancy = false)
                        callback(texture, null)
                    }
                }
            }
            is GPUImage -> {
                TextureMapper.mapTexture(src.texture, texture, "lll1", TargetType.UByteTarget1, callback)
            }
            is ComponentImage -> src.createTexture(texture, sync, checkRedundancy, callback)
            is CachedImage -> createTexture(texture, sync, checkRedundancy, src.base!!, callback)
            is OpaqueImage -> createTexture(texture, sync, checkRedundancy, src.src, callback)
            else -> super.createTexture(texture, sync, checkRedundancy, callback)
        }
    }

    companion object {

        fun getLuminance(r: Byte, g: Byte, b: Byte): Int {
            // 0.2126*r + 0.7152*g + 0.0722*b
            // *2^16, >>16
            return (13941 * r.toInt().and(255)
                    + 46873 * g.toInt().and(255)
                    + 4735 * b.toInt().and(255)) shr 16
        }

        fun getLuminance(rgb: Int): Int {
            val r = rgb.shr(16) and 255
            val g = rgb.shr(8) and 255
            val b = rgb and 255
            // 0.2126*r + 0.7152*g + 0.0722*b
            // *2^16, >>16
            return (13941 * r + 46873 * g + 4735 * b) shr 16
        }
    }
}