package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    HiddenOpenGLContext.createOpenGL()
    val src = getReference("res://icon.png/r.png")
    // todo thumbnails are broken :/
    Thumbs[src, 512, false]!!.write(desktop.getChild("icon-thumbs.png"))
    Engine.requestShutdown()
}

// todo add res:// as a root folder (?) to FileExplorer