package me.anno.image.raw

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image

open class IntImage(
    width: Int, height: Int,
    val data: IntArray = IntArray(width * height),
    hasAlphaChannel: Boolean
) : Image(width, height, if (hasAlphaChannel) 4 else 3, hasAlphaChannel) {

    fun setRGB(x: Int, y: Int, rgb: Int) {
        data[x + y * width] = rgb
    }

    override fun getRGB(index: Int): Int = data[index]

    override fun createTexture(texture: Texture2D, checkRedundancy: Boolean) {
        // data cloning is required, because the function in Texture2D switches the red and blue channels
        if (hasAlphaChannel) {
            texture.createRGBA(cloneData(), checkRedundancy)
        } else {
            texture.createRGBSwizzle(cloneData(), checkRedundancy)
        }
    }

    fun cloneData(): IntArray {
        val clone = IntArray(data.size)
        System.arraycopy(data, 0, clone, 0, data.size)
        return clone
    }

}