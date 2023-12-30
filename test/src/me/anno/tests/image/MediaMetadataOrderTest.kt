package me.anno.tests.image

import me.anno.io.files.InvalidRef
import me.anno.video.ffmpeg.MediaMetadata
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MediaMetadataOrderTest {
    @Test
    fun testOrder() {
        // low order values must be executed first, and when they return true, nothing else must be executed
        var reached = false
        MediaMetadata.registerHandler(0) { _, _ ->
            reached = true
            true
        }
        MediaMetadata.registerHandler(1) { _, _ ->
            throw IllegalStateException()
        }
        MediaMetadata(InvalidRef, null)
        assertTrue(reached)
    }
}