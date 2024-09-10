package me.anno.tests.image

import me.anno.image.raw.ByteImage
import me.anno.image.tar.TGAReader
import me.anno.io.files.FileReference
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {
    convert(OS.downloads)
}

fun convert(file: FileReference) {

    val logger = LogManager.getLogger("TargaTest")

    if (file.isDirectory) {
        for (file2 in file.listChildren()) {
            convert(file2)
        }
    } else {
        if (file.lcExtension == "tga") {
            logger.info("reading file $file")
            val image = TGAReader.read(file.inputStreamSync(), false) as ByteImage
            val data = image.data
            logger.info("${file.name}: ${image.width} x ${image.height}, ${image.numChannels}, ${data.size}")
            // LOGGER.infoOS.desktop)
            val dst = OS.desktop.getChild("tga/${file.name}.png")
            // LOGGER.info("dst: $dst, ${dst.name}")
            // LOGGER.info("dst.parent: ${dst.getParent()}")
            dst.getParent().mkdirs()
            image.write(dst)
        }
    }
}