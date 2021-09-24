package me.anno.utils.test

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.desktop

fun main() {
    for (ref in desktop.listChildren()!!) {
        if (ref.lcExtension == "dds") {
            ImageCPUCache.getImage(ref, false)!!
                .write(getReference(desktop, "${ref.nameWithoutExtension}.png"))
        }
    }
}
