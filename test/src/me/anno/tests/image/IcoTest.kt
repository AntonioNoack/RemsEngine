package me.anno.tests.image

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.desktop
import net.sf.image4j.codec.ico.ICOReader

fun main() {
    // this test now can be directly executed by clicking on an .ico file in the file explorer
    val dst = desktop.getChild("ico")
    dst.tryMkdirs()
    for (source in getReference("C:/Program Files (x86)/Ubisoft/Ubisoft Game Launcher/data/games").listChildren()!!) {
        try {
            ICOReader.readAllLayers(source.inputStream())
                .forEachIndexed { index, image ->
                    image.write(dst.getChild("${source.nameWithoutExtension}.$index.png"))
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}