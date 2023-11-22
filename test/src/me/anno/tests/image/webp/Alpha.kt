package me.anno.tests.image.webp

import me.anno.image.ImageCache
import me.anno.io.files.Signature
import me.anno.utils.Color.a
import me.anno.utils.OS.pictures

fun main() {
    val src = pictures.getChild("atlas.webp")
    val image = ImageCache[src, false]!!.createIntImage()
    println("alpha-pixels: ${image.data.count { it.a() < 255 }}/${image.data.size}")
    for (file in pictures.getChild("Anime").listChildren()!!) {
        Signature.findName(file) { println(it) }
    }
}