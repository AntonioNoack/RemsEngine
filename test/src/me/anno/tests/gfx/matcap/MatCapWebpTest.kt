package me.anno.tests.gfx.matcap

import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.utils.OS.res
import me.anno.utils.Sleep

fun main() {
    // why is this not loading??? -> we deleted the file before it could be processed
    OfficialExtensions.initForTests()
    val source = res.getChild("textures/matcap/BlackPlastic.webp")
    var waiting = true
    ImageCache[source].waitFor {
        println("Image: $it")
        waiting = false
    }
    Sleep.waitUntil("MatCapTest", true) { !waiting }
}