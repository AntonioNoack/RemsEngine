package me.anno.tests.image.thumbs

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.Thumbs
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS

fun main() {
    // todo this is broken... and I don't quite understand why...
    HiddenOpenGLContext.createOpenGL()
    OfficialExtensions.initForTests()
    val src = OS.documents.getChild("Zeitplan.ods")
    val thumbnail = Thumbs[src, 256, false]
    thumbnail?.write(OS.desktop.getChild("${src.nameWithoutExtension}.png"))
    println("thumbnail: $thumbnail")
    Engine.requestShutdown()
}