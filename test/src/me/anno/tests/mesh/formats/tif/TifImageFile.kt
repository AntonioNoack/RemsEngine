package me.anno.tests.mesh.formats.tif

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.utils.OS.pictures
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLoggers("Saveable,ExtensionManager")
    OfficialExtensions.initForTests()
    val source = pictures.getChild("RacerSpec.tif")
    val image = ImageCache[source].waitFor()
    println("Image: $image")
    image?.write(source.getSiblingWithExtension("png"))
}