package me.anno.tests.image

import me.anno.image.Image
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS.desktop
import net.sf.image4j.codec.ico.ICOReader

fun main() {
    // this test now can be directly executed by clicking on an .ico file in the file explorer
    val dst = desktop.getChild("ico")
    dst.tryMkdirs()
    val folder = getReference("C:/Program Files (x86)/Ubisoft/Ubisoft Game Launcher/data/games")
    for (source in folder.listChildren()) {
        try {
            val layers = ICOReader.readAllLayers(source.inputStreamSync()) as List<*>
            layers.forEachIndexed { index, image ->
                image as Image
                image.write(dst.getChild("${source.nameWithoutExtension}.$index.png"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}