package me.anno.tests.cache

import me.anno.engine.OfficialExtensions
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.image.thumbs.ThumbnailCache
import me.anno.io.files.Reference.getReference
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.Sleep

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val source = getReference("E:/Pictures/Anime/Cute/91739.webp")
    var done = false
    ThumbnailCache.getEntry(source, 256).waitFor { image ->
        addGPUTask("Writing Image", 1) {
            image?.write(desktop.getChild(source.name))
            println("Image: $image")
            done = true
        }
    }
    Sleep.waitUntil(true) { done }
}