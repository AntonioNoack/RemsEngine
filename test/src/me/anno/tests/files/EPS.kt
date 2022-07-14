package me.anno.tests.files

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // not currently supported,
    // like svg
    // except with programming code???? ... too complicated for us to handle
    val ref = getReference(downloads, "2d/blank-empty-speech-bubbles-vector-illustration.zip/42894.eps")
    val image = ImageCPUCache.getImage(ref, false)
    LOGGER.info(image)

    println(ref.readText())

    // ffmpeg does not support it either
    /*val tmp = File.createTempFile("ref", ".eps")
    tmp.writeText(ref.readText())
    val meta = getMeta(FileFileRef(tmp), false)
    LOGGER.info(meta)
    tmp.delete()*/

}