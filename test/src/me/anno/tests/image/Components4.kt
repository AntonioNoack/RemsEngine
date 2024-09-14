package me.anno.tests.image

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val src = pictures.getChild("Anime/90940211_p0_master1200.jpg/000a.png")
    val dst = desktop.getChild(src.name)
    val image = ImageCache[src, false]!!
    println(image)
    image.write(dst)
}