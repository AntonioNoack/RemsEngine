package me.anno.tests.image

import me.anno.engine.OfficialExtensions
import me.anno.io.MediaMetadata
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class ImageMetadataTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testImageSize() {
        OfficialExtensions.initForTests()
        val meta = MediaMetadata.getMeta(res.getChild("textures/RGBMask.png")).waitFor()!!
        assertEquals(34, meta.videoWidth)
        assertEquals(33, meta.videoHeight)
        assertEquals(1, meta.videoFrameCount)
    }
}