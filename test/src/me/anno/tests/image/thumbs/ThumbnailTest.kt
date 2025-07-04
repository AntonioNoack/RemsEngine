package me.anno.tests.image.thumbs

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.ThumbnailCache
import me.anno.io.files.Reference.getReference
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertTrue

/**
 * sample file, which was loadable, but which had mysteriously no thumbnail
 * */
fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    assertTrue(getReference("E:/Assets/Megascans/junkyard_high.zip").exists)
    assertTrue(getReference("E:/Assets/Megascans/junkyard_high.zip/wcvodcw").exists)
    assertTrue(getReference("E:/Assets/Megascans/junkyard_high.zip/wcvodcw/wcvodcw_4K_Cavity.jpg").exists)
    val source = getReference("E:/Assets/Megascans/junkyard_high.zip/wcvodcw/wcvodcw_4K_Cavity.jpg")
    assertTrue(source.exists)
    assertTrue(source.length() > 0)
    ThumbnailCache.getEntry(source, 256).waitFor()!!
        .write(desktop.getChild(source.name))
    Engine.requestShutdown()
}