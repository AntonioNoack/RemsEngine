package me.anno.tests.image.webp

import me.anno.image.ImageCPUCache
import me.anno.io.files.Signature
import me.anno.utils.Color.a
import me.anno.utils.OS.pictures

fun main() {
    val src = pictures.getChild("atlas.webp")
    val image = ImageCPUCache[src, false]!!.createIntImage()
    println(image.data.count { it.a() < 255 })
    for (file in pictures.getChild("Anime").listChildren()!!) {
        if (file.lcExtension == "webp") {
            Signature.findName(file) { println(it) }
        }
    }
}