package me.anno.tests.image.thumbs

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.Reference.getReference
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertTrue

fun main() {
    // thumbnail wasn't showing up for audio files
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val ref = getReference(
        "E:/Assets/Torrent/Sonniss.com - GDC 2023 - Game Audio Bundle/" +
                "TheWorkRoom Audio Post - Human Presence In Quiet Rooms/QR001 Human Presence Large Room Busy 5.1.wav"
    )
    assertTrue(ref.exists)
    val texture = Thumbs[ref, 512, false]!!
    texture.write(desktop.getChild("ImageFromWav.jpg"))
    Engine.requestShutdown()
}