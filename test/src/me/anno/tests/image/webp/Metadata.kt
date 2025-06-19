package me.anno.tests.image.webp

import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.OS.pictures
import me.anno.io.MediaMetadata.Companion.getMeta
import javax.imageio.ImageIO

fun main() {
    val src = pictures.getChild("Anime")
    val dst = desktop.getChild("webp")
    dst.tryMkdirs()
    for (file in src.listChildren()) {
        if (file.lcExtension == "webp") {
            println(getMeta(file).waitFor())
            val image = ImageIO.read(file.inputStreamSync())
            dst.getChild(file.nameWithoutExtension + ".png")
                .outputStream().use {
                    ImageIO.write(image, "png", it)
                }
        }
    }
    println(getMeta(downloads.getChild("2d/animated-webp.webp")).waitFor())
}