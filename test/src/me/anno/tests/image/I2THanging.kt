package me.anno.tests.image

import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.image.ImageCache
import me.anno.utils.OS.music

fun main() {
    // done: this hang
    HiddenOpenGLContext.createOpenGL()
    ImageCache[music.getChild("Lost In Space.mp3/rgb.png"), false]
}