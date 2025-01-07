package me.anno.tests.audio

import me.anno.audio.AudioFXCache
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioTimeIndexTest {
    @Test
    fun testGetBufferTime() {
        assertEquals(0.0, AudioFXCache.getTime(0, 123, 546))
        assertEquals(2.0, AudioFXCache.getTime(2, 123, 123))
        assertEquals(0.5, AudioFXCache.getTime(1, 123, 246))
        assertEquals(1.5, AudioFXCache.getTime(1, 0.5, 100, 100))
    }

    @Test
    fun testGetBufferIndex() {
        assertEquals(0.0, AudioFXCache.getIndex(0.0, 123, 546))
        assertEquals(2.0, AudioFXCache.getIndex(2.0, 123, 123))
        assertEquals(0.5, AudioFXCache.getIndex(1.0, 248, 124))
    }
}