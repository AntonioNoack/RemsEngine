package me.anno.tests.image

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.utils.OS
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    val dst = desktop.getChild("gimp")
    dst.tryMkdirs()
    for (file in listOf(
        OS.documents.getChild("Watch Dogs 2 Background.xcf"),
        OS.downloads.getChild("2d/gimp-sample.xcf")
    )) {
        if (!file.exists) throw IllegalStateException("Missing $file")
        val name = file.nameWithoutExtension
        ImageCache[file, false]!!
            .write(dst.getChild("$name.png"))
        for (it in file.listChildren()) {
            ImageCache[it, false]!!
                .write(dst.getChild("$name.${it.nameWithoutExtension}.png"))
        }
    }
}
