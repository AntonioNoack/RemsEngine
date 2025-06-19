package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.gpu.GPUTasks.workGPUTasksUntilShutdown
import me.anno.gpu.texture.TextureCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.downloads
import kotlin.concurrent.thread

fun main() {
    OfficialExtensions.initForTests()
    @Suppress("SpellCheckingInspection")
    val source = downloads.getChild("Abbeanum_new.glb/textures/0.jpg")
    HiddenOpenGLContext.createOpenGL()
    thread {
        println(TextureCache[source].waitFor()!!)
        Engine.requestShutdown()
    }
    workGPUTasksUntilShutdown()
}