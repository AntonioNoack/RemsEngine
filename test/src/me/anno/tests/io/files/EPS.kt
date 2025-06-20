package me.anno.tests.io.files

import me.anno.image.ImageCache
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.io.files.FileFileRef
import me.anno.tests.LOGGER
import me.anno.utils.OS.downloads
import java.io.File

fun main() {

    // not currently supported,
    // like svg
    // except with programming code???? ... too complicated for us to handle
    val ref = downloads.getChild("2d/blank-empty-speech-bubbles-vector-illustration.zip/42894.eps")
    val image = ImageCache[ref].waitFor()
    LOGGER.info(image)

    // ffmpeg does not support it either
    val tmp = File.createTempFile("ref", ".eps")
    tmp.writeText(ref.readTextSync())
    val meta = getMeta(FileFileRef(tmp)).waitFor()
    LOGGER.info(meta)
    tmp.delete()
}