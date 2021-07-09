package me.anno.utils.test

import me.anno.utils.OS
import me.anno.utils.files.Files.use
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() {
    val img = BufferedImage(256, 256, 1)
    for (y in 0 until 256) {
        for (x in 0 until 256) {
            val color = x * 0x10101
            img.setRGB(x, y, color)
        }
    }
    use(OS.desktop.getChild("gradient.png").outputStream()) {
        ImageIO.write(img, "png", it)
    }

}