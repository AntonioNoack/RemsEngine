package me.anno.tests.cache

import me.anno.engine.OfficialExtensions
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.image.thumbs.ThumbnailCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.tests.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.Sleep
import java.util.concurrent.atomic.AtomicInteger

fun main() {

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()

    val src = desktop.getChild("Anime-Test")
    val dst = desktop.getChild("Anime-Thumbs")
    dst.delete()
    dst.tryMkdirs()

    val remainingCtr = AtomicInteger()
    for (file in src.listChildren()) {
        remainingCtr.incrementAndGet()
        ThumbnailCache.getEntry(file, 256)
            .waitFor { texture ->
                if (texture != null) {
                    addGPUTask("t2i", 1) {
                        texture.createImage(flipY = false, withAlpha = true)
                            .write(dst.getChild(file.getNameWithExtension("jpg")))
                        remainingCtr.decrementAndGet()
                    }
                } else {
                    LOGGER.warn("Missing texture for $file")
                    remainingCtr.decrementAndGet()
                }
            }
    }

    Sleep.waitUntil(true) {
        remainingCtr.get() == 0
    }
}