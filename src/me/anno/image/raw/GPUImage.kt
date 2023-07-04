package me.anno.image.raw

import me.anno.gpu.texture.ITexture2D
import me.anno.image.Image
import me.anno.io.files.FileReference

class GPUImage(val texture: ITexture2D, numChannels: Int, hasAlphaChannel: Boolean, val hasOwnership: Boolean) :
    Image(texture.width, texture.height, numChannels, hasAlphaChannel) {

    override fun getRGB(index: Int): Int {
        throw RuntimeException("GPUImage.getRGB() is not implemented")
    }

    override fun createBufferedImage() =
        texture.createImage(false, hasAlphaChannel).createBufferedImage()

    override fun write(dst: FileReference) {
        texture.createImage(false, hasAlphaChannel).write(dst)
    }

    override fun createIntImage() = texture.createImage(false, hasAlphaChannel)

}