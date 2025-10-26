package me.anno.tests.image

import me.anno.io.MediaMetadata
import me.anno.utils.OS.pictures
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull
import me.anno.video.FFMPEGMetadata.loadFFMPEG
import org.joml.Vector2i

fun main() {
    WebpSize.register()
    for (file in pictures.getChild("Anime").listChildren()) {
        if (file.lcExtension != "webp") continue
        val custom = file.inputStreamSync().use { WebpSize.getWebpImageSize(it) } as? Vector2i
        val expected = MediaMetadata(file, null, 0).apply { loadFFMPEG() }
        println("${file.nameWithoutExtension}: $custom, ${expected.videoWidth} x ${expected.videoHeight}")
        assertNotNull(custom)
        assertEquals(expected.videoWidth, custom?.x)
        assertEquals(expected.videoHeight, custom?.y)
    }
}