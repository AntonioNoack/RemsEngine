package me.anno.tests.image

import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.image.ImageGPUCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import me.anno.utils.OS.videos

fun main() {
    // why is loading these images within materials not working?
    // todo why is their background fully transparent???
    val dst = desktop.getChild("tmp")
    dst.mkdirs()
    HiddenOpenGLContext.createOpenGL()
    for (file in pictures.getChild("Anime").listChildren()!!) {
        if (file.name == "70697252_p4_master1200.webp") if (file.lcExtension == "webp" && !file.isDirectory) {
            ImageGPUCache[file, false]!!
                .write(dst.getChild("${file.nameWithoutExtension}.png"), flipY = false, withAlpha = false)
        }
    }
    // todo why is video not working there either?
    if (false) for (file in videos.listChildren()!!) {
        if (!file.isDirectory) {
            ImageGPUCache[file, false]!!
                .write(dst.getChild("${file.nameWithoutExtension}.png"))
        }
    }
}