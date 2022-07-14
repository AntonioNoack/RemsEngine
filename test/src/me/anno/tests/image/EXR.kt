package me.anno.tests.image

import me.anno.image.exr.EXRReader
import me.anno.utils.OS
import java.io.IOException

fun main() {
    val dst = OS.desktop.getChild("exr")
    dst.tryMkdirs()
    OS.downloads.getChild("2d/EXR samples.zip")
        .listChildren()!!
        .filter { it.lcExtension == "exr" }
        .forEach {
            try {
                EXRReader.read(it).write(dst.getChild("${it.nameWithoutExtension}.png"))
            } catch (e: IOException) {
                IOException(it.absolutePath, e)
                    .printStackTrace()
            }
        }
}
