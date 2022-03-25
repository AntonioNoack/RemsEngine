package me.anno.image.raw

import me.anno.config.DefaultStyle.black
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import kotlin.math.min

open class OpaqueImage(
    val src: Image
) : Image(src.width, src.height, min(3, src.numChannels), false) {

    override fun getWidth(): Int = src.getWidth()
    override fun getHeight(): Int = src.getHeight()

    override fun getRGB(index: Int): Int = src.getRGB(index) or black

    override fun createTexture(texture: Texture2D, checkRedundancy: Boolean) {
        if (!src.hasAlphaChannel()) {
            src.createTexture(texture, checkRedundancy)
        } else {
            when (src) {
                is IntImage -> {
                    val clone = src.cloneData()
                    texture.createRGBSwizzle(clone, checkRedundancy)
                    Texture2D.intArrayPool.returnBuffer(clone)
                }
                is ByteImage -> {
                    when (src.numChannels) {
                        1, 2, 3 -> src.createTexture(texture, checkRedundancy)
                        4 -> {
                            val width = src.width
                            val height = src.height
                            val cloned = Texture2D.bufferPool[3 * width * height, false]
                            var j = 0
                            val data = src.data
                            for (i in 0 until width * height) {
                                j++ // skip alpha
                                cloned.put(data[j++])
                                cloned.put(data[j++])
                                cloned.put(data[j++])
                            }
                            cloned.flip()
                            texture.createRGB(cloned, checkRedundancy)
                            // buffer is returned automatically
                        }
                        else -> throw RuntimeException("Cannot create OpaqueImage from image with more than 4 channels")
                    }
                }
                else -> super.createTexture(texture, checkRedundancy)
            }
        }
    }

}