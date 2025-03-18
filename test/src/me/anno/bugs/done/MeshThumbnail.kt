package me.anno.bugs.done

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.Thumbs
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    // we had a bug that the prefab wasn't delegated properly
    HiddenOpenGLContext.createOpenGL()
    OfficialExtensions.initForTests()
    val src = downloads.getChild("3d/emilia-rezero.glb")
    val dst = desktop.getChild(src.getNameWithExtension("jpg"))
    val thumb = Thumbs[src, 512, false]
    thumb?.write(dst)
    Engine.requestShutdown()
    println(thumb)
}