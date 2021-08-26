package me.anno.utils

import me.anno.io.files.FileReference
import org.apache.commons.imaging.Imaging
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

val progress = OS.documents.getChild("IdeaProjects/VideoStudio/progress")

fun main() {
    // this works, but does it work everywhere??...
    val fileNames = listOf(
        "cubemap.png", // both
        "jpeg2000.jp2", // ImageIO
        "cubemap.hdr", // Imaging
        "crashed by unpack_alignment.webp", // only FFMPEG
        "fav128.ico" // Imaging
    )
    fileNames.forEach {
        val file = progress.getChild(it)
        test(file, ::imageIOTest)
        test(file, ::imagingTest)
    }
}

fun test(file: FileReference, loadFunc: (file: FileReference) -> BufferedImage) {
    try {
        val image = loadFunc(file)
        LOGGER.info(image.run { "${file.name}: $width x $height, ${image.colorModel}" })
    } catch (e: Exception) {
        LOGGER.info("${file.name}: ${e.message}")
    }
}

fun imageIOTest(file: FileReference): BufferedImage {
    return ImageIO.read(file.inputStream())
}

fun imagingTest(file: FileReference): BufferedImage {
    return Imaging.getBufferedImage(file.inputStream())
}