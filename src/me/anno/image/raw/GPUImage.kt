package me.anno.image.raw

import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.texture.ITexture2D
import me.anno.image.Image
import java.awt.image.BufferedImage

class GPUImage(val texture: ITexture2D, numChannels: Int, hasAlphaChannel: Boolean, val hasOwnership: Boolean) :
    Image(texture.w, texture.h, numChannels, hasAlphaChannel) {

    override fun getRGB(index: Int): Int {
        throw RuntimeException("GPUImage.getRGB() is not implemented")
    }

    override fun createBufferedImage(): BufferedImage {
        return FramebufferToMemory
            .createImage(texture, false, hasAlphaChannel)
            .createBufferedImage()
    }

}