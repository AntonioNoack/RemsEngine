package me.anno.tests.image

import me.anno.image.ImageCache
import me.anno.utils.OS

fun main() {
    for (file in listOf(
        OS.documents.getChild("Watch Dogs 2 Background.xcf"),
        OS.downloads.getChild("2d/gimp-sample.xcf")
    )) {
        val name = file.nameWithoutExtension
        ImageCache[file, false]!!
            .write(OS.desktop.getChild("$name.png"))
        for (it in file.listChildren()) {
            ImageCache[it, false]!!
                .write(OS.desktop.getChild("$name.${it.nameWithoutExtension}.png"))
        }
    }
}
