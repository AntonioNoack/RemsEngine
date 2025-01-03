package me.anno.tests.mesh.obj

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.Thumbs
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    // thumbnail generation was failing, because the file in question doesn't have a typical start
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val source = downloads.getChild("3d/ogldev-source/dabrovic-sponza/sponza.obj")
    val thumbnail = Thumbs[source, 512, false]!!
    thumbnail.write(desktop.getChild("dabrovic-sponza.png"))
    Engine.requestShutdown()
}