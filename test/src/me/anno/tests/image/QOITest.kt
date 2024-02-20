package me.anno.tests.image

import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.image.thumbs.Thumbs
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    HiddenOpenGLContext.createOpenGL()
    val src = downloads.getChild("2d/qoi_test_images.zip/qoi_test_images/testcard_rgba.qoi")
    val dst = desktop
    var size = 64
    while (size <= 256) {
        Thumbs[src, size, false]!!.createImage(true, true)
            .write(dst.getChild("$size.png"))
        size *= 2
    }
}