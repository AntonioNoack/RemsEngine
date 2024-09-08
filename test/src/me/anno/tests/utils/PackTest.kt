package me.anno.tests.utils

import me.anno.Engine
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference
import me.anno.io.packer.Packer
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {

    OfficialExtensions.initForTests()
    val logger = LogManager.getLogger("PackTest")

    logger.info("Start")

    val resources = listOf(
        OS.pictures.getChild("fav128.png"),
        OS.documents.getChild("sphere.obj"),
        OS.downloads.getChild("3d/vampire.zip/dancing_vampire.dae"),
        getReference("http://phychi.com/img/fav16.png")
        // todo ftp/sftp resources?
    )

    val dst = OS.desktop.getChild("packingTest.zip")
    val map = Packer.packWithReporting(resources, true, dst, true, 500)
    for ((key, value) in map) {
        logger.info("$value: $key")
    }

    logger.info(dst.listChildren())
    logger.info("End")
    Engine.requestShutdown()
}