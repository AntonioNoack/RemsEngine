package me.anno.tests.image

import me.anno.io.MediaMetadata
import me.anno.io.files.InvalidRef
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaMetadataOrderTest {
    @Test
    fun testOrder() {
        // low order values must be executed first, and when they return true, nothing else must be executed
        var reached = false
        MediaMetadata.registerHandler(0, "x") { _, _, _ ->
            reached = true
            true
        }
        MediaMetadata.registerHandler(1, "y") { _, _, _ ->
            throw IllegalStateException()
        }
        MediaMetadata(InvalidRef, null, 0)
        assertTrue(reached)
    }
}