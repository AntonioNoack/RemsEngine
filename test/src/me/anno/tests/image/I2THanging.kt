package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.jvm.HiddenOpenGLContext
import me.anno.image.ImageCache
import me.anno.utils.OS.music

fun main() {
    // done: this hang
    OfficialExtensions.register()
    ExtensionLoader.load()
    HiddenOpenGLContext.createOpenGL()
    ImageCache[music.getChild("Lost In Space.mp3/rgb.png"), false]!!
    Engine.requestShutdown()
}