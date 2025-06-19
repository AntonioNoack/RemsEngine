package me.anno.tests.physics.fluid

import me.anno.image.ImageCache
import me.anno.utils.OS

fun main() {
    val source = OS.pictures.getChild("atlas.webp")
    println(ImageCache[source].waitFor())
}