package me.anno.image.raw

import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import kotlin.math.min

open class OpaqueImage(
    val src: Image
) : Image(src.width, src.height, min(3, src.numChannels), false) {

    override var width: Int
        get() = src.width
        set(value) {
            src.width = value
        }

    override var height: Int
        get() = src.height
        set(value) {
            src.height = value
        }

    override fun getRGB(index: Int): Int = src.getRGB(index) or black

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        if (!src.hasAlphaChannel) {
            src.createTexture(texture, sync, checkRedundancy)
        } else {
            when (src) {
                is IntImage -> {
                    val data = src.cloneData()
                    val data2 = if (checkRedundancy) texture.checkRedundancy(data) else data
                    if (sync && GFX.isGFXThread()) texture.createBGR(data2, false)
                    else GFX.addGPUTask("OpaqueImage", width, height) {
                        texture.createBGR(data2, false)
                    }
                }
                is ByteImage -> {
                    when (src.numChannels) {
                        1, 2, 3 -> src.createTexture(texture, sync, checkRedundancy)
                        4 -> {
                            val data = src.data
                            val buffer = Texture2D.bufferPool[data.size, false, false]
                            if (src.rgba) {
                                buffer.put(data)
                            } else {// argb, must convert from it to rgba
                                for (i in 0 until texture.w * texture.h * 4 step 4) {
                                    buffer.put(data[i+1]) // r
                                    buffer.put(data[i+2]) // g
                                    buffer.put(data[i+3]) // b
                                    buffer.put(-1) // a, doesn't matter
                                }
                            }
                            buffer.flip()
                            if (checkRedundancy) texture.checkRedundancy(buffer)
                            // to do check whether this is correct; should be correct :)
                            if (sync && GFX.isGFXThread()) {
                                texture.create(TargetType.UByteTarget3, TargetType.UByteTarget4, buffer)
                                Texture2D.bufferPool.returnBuffer(buffer)
                            } else GFX.addGPUTask("OpaqueImage", width, height) {
                                texture.create(TargetType.UByteTarget3, TargetType.UByteTarget4, buffer)
                                Texture2D.bufferPool.returnBuffer(buffer)
                            }
                        }
                        else -> throw RuntimeException("Cannot create OpaqueImage from image with more than 4 channels")
                    }
                }
                else -> super.createTexture(texture, sync, checkRedundancy)
            }
        }
    }

}