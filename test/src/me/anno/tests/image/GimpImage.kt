package me.anno.tests

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference
import me.anno.utils.OS

fun main() {
    for (file in listOf(
        OS.documents.getChild("Watch Dogs 2 Background.xcf"),
        OS.downloads.getChild("2d/gimp-sample.xcf")
    )) {
        val name = file.nameWithoutExtension
        ImageCPUCache.getImage(file, false)!!
            .write(FileReference.getReference(OS.desktop, "$name.png"))
        for (it in file.listChildren()!!) {
            ImageCPUCache.getImage(it, false)!!
                .write(FileReference.getReference(OS.desktop, "$name.${it.nameWithoutExtension}.png"))
        }
    }
}
