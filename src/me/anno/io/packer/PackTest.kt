package me.anno.io.packer

import me.anno.utils.LOGGER
import me.anno.utils.OS

fun main() {

    LOGGER.info("Start")

    val resources = listOf(
        OS.downloads.getChild("warning.png"),
        OS.documents.getChild("sphere.obj"),
        OS.downloads.getChild("vampire.zip/dancing_vampire.dae"),
        // todo test online resources, when we support them
        // todo ftp/sftp resources?
    )

    val dst = OS.downloads.getChild("packingTest.zip")
    val map = Packer.packWithReporting(resources, true, dst, true, 500)
    for ((key, value) in map) {
        println("$value: $key")
    }

    println(dst.listChildren())

    LOGGER.info("End")

}