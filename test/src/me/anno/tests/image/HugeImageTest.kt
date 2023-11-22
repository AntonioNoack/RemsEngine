package me.anno.tests.image

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.TextureCache
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep.waitUntilDefined
import kotlin.concurrent.thread

fun main() {
    @Suppress("SpellCheckingInspection")
    val source = downloads.getChild("Abbeanum_new.glb/textures/0.jpg")
    HiddenOpenGLContext.createOpenGL()
    thread {
        val tex = waitUntilDefined(true) {
            TextureCache[source, false]
        }
        println(tex)
        Engine.requestShutdown()
    }
    GFX.workGPUTasksUntilShutdown()
}