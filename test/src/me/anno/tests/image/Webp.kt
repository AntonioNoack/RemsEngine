package me.anno.tests.image

import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.image.ImageGPUCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import me.anno.utils.OS.videos

fun main() {
    // todo why is loading these images within materials not working?
    val dst = desktop.getChild("tmp")
    dst.mkdirs()
    HiddenOpenGLContext.createOpenGL()
    for (file in pictures.getChild("Anime").listChildren()!!) {
        if (file.lcExtension == "webp" && !file.isDirectory) {
            ImageGPUCache.getImage(file, false)!!
                .write(dst.getChild("${file.nameWithoutExtension}.png"))
        }
    }
    // todo why is video not working there either?
    for (file in videos.listChildren()!!) {
        if (!file.isDirectory) {
            ImageGPUCache.getImage(file, false)!!
                .write(dst.getChild("${file.nameWithoutExtension}.png"))
        }
    }
}