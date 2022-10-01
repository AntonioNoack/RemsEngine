package me.anno.image.raw

import me.anno.io.files.FileReference
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

// todo if image is grayscale, produce grayscale image
fun BufferedImage.toImage() = IntImage(
    width, height,
    getRGB(0, 0, width, height, null, 0, width),
    colorModel.hasAlpha()
)

fun BufferedImage.write(dst: FileReference) {
    val format = dst.lcExtension
    dst.outputStream().use { out ->
        ImageIO.write(this, format, out)
    }
}