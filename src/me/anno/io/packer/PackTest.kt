package me.anno.io.packer

import me.anno.io.files.FileReference
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {

    val logger = LogManager.getLogger("PackTest")

    logger.info("Start")

    val resources = listOf(
        OS.pictures.getChild("fav128.png"),
        OS.documents.getChild("sphere.obj"),
        OS.downloads.getChild("3d/vampire.zip/dancing_vampire.dae"),
        FileReference.getReference("http://phychi.com/img/fav16.png")
        // todo ftp/sftp resources?
    )

    val dst = OS.downloads.getChild("packingTest.zip")
    val map = Packer.packWithReporting(resources, true, dst, true, 500)
    for ((key, value) in map) {
        logger.info("$value: $key")
    }

    logger.info(dst.listChildren())

    logger.info("End")

}