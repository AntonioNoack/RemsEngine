package me.anno.tests.image

import me.anno.io.files.FileReference
import me.anno.utils.OS.ideProjects
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

fun main() {

    val logger = LogManager.getLogger("JP2ImageIOTest")

    fun test(file: FileReference, loadFunc: (file: FileReference) -> BufferedImage) {
        try {
            val image = loadFunc(file)
            logger.info(image.run { "${file.name}: $width x $height, ${image.colorModel}" })
        } catch (e: Exception) {
            logger.info("${file.name}: ${e.message}")
        }
    }

    fun imageIOTest(file: FileReference): BufferedImage {
        return ImageIO.read(file.inputStreamSync())
    }

    fun imagingTest(file: FileReference): BufferedImage {
        return Imaging.getBufferedImage(file.inputStreamSync())
    }

    val progress = ideProjects.getChild("RemsStudio/progress")

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
