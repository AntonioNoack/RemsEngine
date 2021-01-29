package me.anno.image

import java.awt.image.BufferedImage

object Images {
    fun BufferedImage.withoutAlpha(): BufferedImage {
        if (!colorModel.hasAlpha()) return this
        val dst = BufferedImage(width, height, 1)
        for (x in 0 until width) {
            for (y in 0 until height) {
                dst.setRGB(x, y, getRGB(x, y))
            }
        }
        return dst
    }
}