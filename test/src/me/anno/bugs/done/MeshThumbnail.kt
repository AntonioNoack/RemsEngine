package me.anno.bugs.done

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.ThumbnailCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    // we had a bug that the prefab wasn't delegated properly
    HiddenOpenGLContext.createOpenGL()
    OfficialExtensions.initForTests()
    val src = downloads.getChild("3d/emilia-rezero.glb")
    val dst = desktop.getChild(src.getNameWithExtension("jpg"))
    val thumb = ThumbnailCache.getEntry(src, 512).waitFor()
    thumb?.write(dst, flipY = false)
    Engine.requestShutdown()
    println(thumb)
}