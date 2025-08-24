package me.anno.tests.cache

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS.desktop
import me.anno.utils.Sleep

fun main() {
    OfficialExtensions.initForTests()
    val source = getReference("E:/Pictures/Anime/Cute/91739.webp")
    var done = false
    ImageCache[source].waitFor { image ->
        image?.write(desktop.getChild(source.name))
        println("Image: $image")
        done = true
    }
    Sleep.waitUntil(true) { done }
}