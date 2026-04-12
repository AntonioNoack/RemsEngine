package me.anno.tests.image.exr

import me.anno.image.Image
import me.anno.image.exr.OpenEXRReader
import me.anno.image.exr.TinyEXRReader
import me.anno.io.files.Signature
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("EXRsFromPolyHaven")

fun main() {
    val source = pictures.getChild("Textures/marble_cliff_01_4k.blend/textures/marble_cliff_01_rough_4k.exr")
    val sourceBytes = source.readBytesSync()

    val signature = Signature.findName(sourceBytes)
    println("Signature: $signature")

    try {
        // try TinyEXR
        val tinyImage = TinyEXRReader.read(sourceBytes.inputStream())
        LOGGER.info("TinyEXR-Success: $tinyImage")
    } catch (e: Exception) {
        LOGGER.warn("TinyEXR-Failure", e)
    }

    println("bytes: ${sourceBytes.take(10)}")

    try {
        LogManager.logAll()
        // try our OpenEXR sample
        val exrImage = OpenEXRReader.readImage(sourceBytes) as Image
        LOGGER.info("OpenEXR-Success: $exrImage")
        exrImage.write(desktop.getChild("exr.jpg"))
    } catch (e: Exception) {
        LOGGER.warn("OpenEXR-Failure", e)
    }
}