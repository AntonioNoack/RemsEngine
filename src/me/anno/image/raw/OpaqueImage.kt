package me.anno.image.raw

import me.anno.utils.async.Callback
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.utils.Color.black
import kotlin.math.min

/**
 * turns any image into an image without alpha channel
 * */
open class OpaqueImage(val src: Image) :
    Image(src.width, src.height, min(3, src.numChannels), false) {

    // proxy width, probably shouldn't be set
    override var width: Int
        get() = src.width
        set(value) {
            src.width = value
        }

    // proxy height, probably shouldn't be set
    override var height: Int
        get() = src.height
        set(value) {
            src.height = value
        }

    override fun getRGB(index: Int): Int = src.getRGB(index) or black

    override fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        if (!src.hasAlphaChannel) {
            src.createTexture(texture, sync, checkRedundancy, callback)
        } else {
            when (src) {
                is IntImage -> {
                    IntImage(width, height, src.data, false)
                        .createTexture(texture, sync, checkRedundancy, callback)
                }
                is GPUImage -> {
                    TextureMapper.mapTexture(src.texture, texture, "rgb1", TargetType.UInt8x4, callback)
                }
                is ByteImage -> {
                    val data = src.data
                    val buffer = Texture2D.bufferPool[data.size, false, false]
                    when (src.format) {
                        ByteImage.Format.RGBA -> {
                            buffer.put(data)
                        }
                        ByteImage.Format.BGRA -> {
                            for (i in 0 until texture.width * texture.height * 4 step 4) {
                                buffer.put(data[i + 2]) // r
                                buffer.put(data[i + 1]) // g
                                buffer.put(data[i]) // b
                                buffer.put(-1) // a, doesn't matter
                            }
                        }
                        ByteImage.Format.ARGB -> {
                            // must convert from it to rgba
                            for (i in 0 until texture.width * texture.height * 4 step 4) {
                                buffer.put(data[i + 1]) // r
                                buffer.put(data[i + 2]) // g
                                buffer.put(data[i + 3]) // b
                                buffer.put(-1) // a, doesn't matter
                            }
                        }
                        else -> throw NotImplementedError()
                    }
                    buffer.flip()
                    if (checkRedundancy) texture.checkRedundancy(buffer)
                    // to do check whether this is correct; should be correct :)
                    if (sync && GFX.isGFXThread()) {
                        texture.create(TargetType.UInt8x3, TargetType.UInt8x4, buffer)
                        Texture2D.bufferPool.returnBuffer(buffer)
                        callback.ok(texture)
                    } else GFX.addGPUTask("OpaqueImage", width, height) {
                        texture.create(TargetType.UInt8x3, TargetType.UInt8x4, buffer)
                        Texture2D.bufferPool.returnBuffer(buffer)
                        callback.ok(texture)
                    }
                }
                else -> super.createTexture(texture, sync, checkRedundancy, callback)
            }
        }
    }
}