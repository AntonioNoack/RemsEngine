package me.anno.tests.image.webp

import me.anno.Engine
import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.texture.TextureCache
import me.anno.image.ImageCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

fun main() {
    val src = pictures.getChild("Anime/64bb3b17c22af131d67f11a1a1adb258.webp")
    val name = src.nameWithoutExtension
    HiddenOpenGLContext.createOpenGL()
    ImageCache[src].waitFor()!!.write(desktop.getChild("$name-cpu.png"))
    TextureCache[src].waitFor()!!.write(desktop.getChild("$name-gpu.png"))
    Engine.requestShutdown()
}