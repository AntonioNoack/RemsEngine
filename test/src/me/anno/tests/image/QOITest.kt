package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.jvm.HiddenOpenGLContext
import me.anno.image.thumbs.Thumbs
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val src = downloads.getChild("2d/qoi_test_images.zip/qoi_test_images/testcard_rgba.qoi")
    val dst = desktop.getChild("qoi")
    dst.tryMkdirs()
    for (size in listOf(64, 128, 256)) {
        val texture = Thumbs[src, size, false] ?: throw IllegalStateException("Missing thumbs for $size")
        texture
            .createImage(flipY = false, withAlpha = true)
            .write(dst.getChild("$size.png"))
    }
    Engine.requestShutdown()
}