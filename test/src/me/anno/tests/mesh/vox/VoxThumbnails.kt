package me.anno.tests.mesh.vox

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.ThumbnailCache
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

/**
 * load all MagicaVoxel samples, and generate thumbnails from that
 * */
fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    // test thumbnail generation
    val source = downloads.getChild("MagicaVoxel/vox")
    val destination = desktop.getChild("vox2png")
    destination.tryMkdirs()
    for (file in source.listChildren()) {
        if (file.lcExtension != "vox") continue
        ThumbnailCache.getEntry(file, 512).waitFor()!!
            .write(
                destination.getChild("${file.nameWithoutExtension}.png"),
                flipY = false, withAlpha = true
            )
    }
    Engine.requestShutdown()
}