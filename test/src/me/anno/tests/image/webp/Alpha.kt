package me.anno.tests.image.webp

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.io.files.SignatureCache
import me.anno.utils.Color.a
import me.anno.utils.OS.pictures

fun main() {
    OfficialExtensions.initForTests()
    val src = pictures.getChild("textures/atlas.webp")
    val image = ImageCache[src, false]!!.asIntImage()
    println("alpha-pixels: ${image.data.count { it.a() < 255 }}/${image.data.size}")
    for (file in pictures.getChild("Anime").listChildren()) {
        SignatureCache.getAsync(file) { println(it?.name) }
    }
}