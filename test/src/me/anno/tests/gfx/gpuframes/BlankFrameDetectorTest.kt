package me.anno.tests.gfx.gpuframes

import me.anno.utils.Color
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.video.formats.gpu.BlankFrameDetector
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class BlankFrameDetectorTest {
    @Test
    fun testBlankFrames() {
        val white = createFrame(Color.white)
        val black = createFrame(Color.black)
        assertFalse(isBlankFrame(white, white, white))
        assertFalse(isBlankFrame(black, black, black))
        assertFalse(isBlankFrame(white, white, black))
        assertFalse(isBlankFrame(black, black, white))
        assertTrue(isBlankFrame(white, black, white))
        assertTrue(isBlankFrame(black, white, black))
    }

    fun createFrame(color: Int): BlankFrameDetector {
        val detector = BlankFrameDetector()
        val bytes = ByteBuffer.allocateDirect(4)
        bytes.putInt(color).flip()
        detector.putRGBA(bytes)
        return detector
    }

    fun isBlankFrame(c0: BlankFrameDetector, c2: BlankFrameDetector, c4: BlankFrameDetector): Boolean {
        return c2.isBlankFrame(c0, c4)
    }
}