package me.anno.tests.image

import me.anno.engine.OfficialExtensions
import me.anno.io.MediaMetadata
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class ImageMetadataTest {
    @Test
    fun testImageSize() {
        OfficialExtensions.initForTests()
        val meta = MediaMetadata.getMeta(res.getChild("textures/RGBMask.png"), false)!!
        assertEquals(34, meta.videoWidth)
        assertEquals(33, meta.videoHeight)
        assertEquals(1, meta.videoFrameCount)
    }
}