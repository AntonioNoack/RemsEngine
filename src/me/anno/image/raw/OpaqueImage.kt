package me.anno.image.raw

import me.anno.config.DefaultStyle.black
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import kotlin.math.min

open class OpaqueImage(
    val src: Image
) : Image(src.width, src.height, min(3, src.numChannels), false) {

    override fun getRGB(index: Int): Int = src.getRGB(index) or black

    override fun createTexture(texture: Texture2D, checkRedundancy: Boolean) {
        if (!src.hasAlphaChannel()) {
            src.createTexture(texture, checkRedundancy)
        } else {
            when (src) {
                is IntImage -> texture.createRGBSwizzle(src.cloneData(), checkRedundancy)
                is ByteImage -> texture.createRGB(src.data, checkRedundancy)
                else -> super.createTexture(texture, checkRedundancy)
            }
        }
    }

}