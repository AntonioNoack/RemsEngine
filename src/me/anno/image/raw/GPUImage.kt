package me.anno.image.raw

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.io.files.FileReference

class GPUImage(val texture: ITexture2D, numChannels: Int, hasAlphaChannel: Boolean) :
    Image(texture.width, texture.height, numChannels, hasAlphaChannel) {

    constructor(texture: ITexture2D, numChannels: Int) : this(texture, numChannels, numChannels > 3)
    constructor(texture: Texture2D) : this(texture, texture.numChannels)

    override fun getRGB(index: Int): Int {
        throw RuntimeException("GPUImage.getRGB() is not implemented")
    }

    override fun createBufferedImage() =
        texture.createImage(false, hasAlphaChannel).createBufferedImage()

    override fun write(dst: FileReference) {
        texture.createImage(false, hasAlphaChannel).write(dst)
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        TODO("copy texture using shader")
    }

    override fun createIntImage() = texture.createImage(false, hasAlphaChannel)
}