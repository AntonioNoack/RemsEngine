package me.anno.utils

import org.apache.commons.imaging.Imaging
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

val progress = File(OS.documents.unsafeFile, "IdeaProjects/VideoStudio/progress")

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
        val file = File(progress, it)
        test(file, ::imageIOTest)
        test(file, ::imagingTest)
    }
}

fun test(file: File, loadFunc: (file: File) -> BufferedImage) {
    try {
        val image = loadFunc(file)
        println(image.run { "${file.name}: $width x $height, ${image.colorModel}" })
    } catch (e: Exception) {
        println("${file.name}: ${e.message}")
    }
}

fun imageIOTest(file: File): BufferedImage {
    return ImageIO.read(file)
}

fun imagingTest(file: File): BufferedImage {
    return Imaging.getBufferedImage(file)
}