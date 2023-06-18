package me.anno.tests.image.webp

import me.anno.Engine
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

fun main() {
    val src = pictures.getChild("Anime/64bb3b17c22af131d67f11a1a1adb258.webp")
    val name = "testImage"
    HiddenOpenGLContext.createOpenGL()
    ImageCPUCache[src, false]!!.write(desktop.getChild("$name-cpu.png"))
    ImageGPUCache[src, false]!!.write(desktop.getChild("$name-gpu.png"))
    Engine.requestShutdown()
}