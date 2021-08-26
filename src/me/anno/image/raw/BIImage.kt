package me.anno.image.raw

import java.awt.image.BufferedImage

class BIImage(val src: BufferedImage) : IntImage(
    src.width, src.height,
    src.getRGB(0, 0, src.width, src.height, null, 0, src.width),
    src.colorModel.hasAlpha()
) {

    init {
        width = src.width
        height = src.height
    }

    override fun createBufferedImage(): BufferedImage = src

}