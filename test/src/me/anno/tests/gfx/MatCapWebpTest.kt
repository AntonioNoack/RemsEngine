package me.anno.tests.gfx

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.res
import me.anno.utils.Sleep

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val source = res.getChild("textures/matcap/BlackPlastic.webp")
    var waiting = true
    ImageCache[source].waitFor {
        println("Image: $it")
        waiting = false
    }
    Sleep.waitUntil("MatCapTest", true) { !waiting }
}