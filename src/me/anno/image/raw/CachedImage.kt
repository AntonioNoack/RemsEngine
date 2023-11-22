package me.anno.image.raw

import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.io.files.FileReference
import java.awt.image.BufferedImage

/**
 * when you don't want to store references in memory all the time, use this class
 * */
class CachedImage(val src: FileReference, w: Int, h: Int, numChannels: Int, hasAlpha: Boolean) :
    Image(w, h, numChannels, hasAlpha) {

    @Suppress("unused")
    constructor(src: FileReference, image: Image) :
            this(src, image.width, image.height, image.numChannels, image.hasAlphaChannel)

    /** cache timeout in milli seconds */
    var timeout = 50L

    val base get() = ImageCache[src, timeout, false]

    override fun getRGB(index: Int): Int {
        val img = base!!
        width = img.width
        height = img.height
        return img.getRGB(index)
    }

    override fun createBufferedImage(): BufferedImage {
        return base!!.createBufferedImage()
    }

    override fun createBufferedImage(dstWidth: Int, dstHeight: Int, allowUpscaling: Boolean): BufferedImage {
        return base!!.createBufferedImage(dstWidth, dstHeight, allowUpscaling)
    }

    override fun createIntImage(): IntImage {
        return base!!.createIntImage()
    }

    override fun createTexture(texture: Texture2D, sync: Boolean, checkRedundancy: Boolean) {
        base!!.createTexture(texture, sync, checkRedundancy)
    }

}