package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.io.MediaMetadata
import me.anno.utils.OS.pictures

fun main() {
    // verify that ImageIO is only called for animated webp
    // -> yes :)
    WebpSize.register()
    OfficialExtensions.initForTests()
    for (file in pictures.getChild("Anime").listChildren()) {
        if (file.lcExtension != "webp") continue
        val meta = MediaMetadata.getMeta(file, false)!!
        println("${file.nameWithoutExtension}: ${meta.videoWidth} x ${meta.videoHeight}")
    }
    Engine.requestShutdown()
}