package me.anno.tests.audio

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.audio.AudioFXCache
import me.anno.audio.AudioFXCache.SPLITS
import me.anno.cache.CacheSection
import me.anno.engine.OfficialExtensions
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.tests.FlakyTest
import me.anno.tests.audio.AudioReaderTest.Companion.createStereoAudioFile
import me.anno.tests.audio.AudioReaderTest.Companion.numSamples
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class AmplitudeRangeTest {

    val duration = 0.1

    fun createAudioInRange(dst: FileReference, min: Float, max: Float) {
        val scaleUp = AudioReaderTest.scale * (max - min) / (2.0 * numSamples(duration))
        val offsetUp = AudioReaderTest.scale * min
        val scaleDown = -scaleUp
        val offsetDown = AudioReaderTest.scale * max
        createStereoAudioFile(dst, duration, {
            (it * scaleUp + offsetUp).toInt().toShort()
        }, {
            (it * scaleDown + offsetDown).toInt().toShort()
        })
    }

    // todo this test is always getting stuck :/
   //@Test
    @FlakyTest
    @Execution(ExecutionMode.SAME_THREAD)
    fun testAmplitudeRange() {
        OfficialExtensions.initForTests()
        val file = FileFileRef.createTempFile("audio", "wav")
        createAudioInRange(file, -1f, 1f)
        val range = AudioFXCache.getRange(
            file, 4096, 0, numSamples(duration).toLong(), LoopingState.PLAY_ONCE,
            "x"
        ).waitFor()!!.value
        assertEquals(SPLITS * 2, range.size)
        val threshold = 2 * (AudioReaderTest.scale / SPLITS).toInt()
        for (i in 0 until SPLITS) {
            val expectedValue = (AudioReaderTest.scale * (SPLITS - i) / SPLITS).toInt()
            assertEquals(-expectedValue, range[i * 2].toInt(), threshold)
            assertEquals(+expectedValue, range[i * 2 + 1].toInt(), threshold)
        }
        Engine.requestShutdown()
    }
}