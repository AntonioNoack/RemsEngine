package me.anno.tests.audio

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.audio.AudioCache.playbackSampleRate
import me.anno.audio.AudioData
import me.anno.audio.AudioFXCache
import me.anno.cache.Promise
import me.anno.engine.OfficialExtensions
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class AudioReaderTest {

    private val noise = PerlinNoise(1234L, 8, 0.5f, -scale, +scale)
    fun waveForm(t: Float): Short {
        return (noise[t * 64f]).toInt().toShort()
    }

    fun waveForm(i: Int): Short {
        return waveForm(i / sampleRate)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun writeReadSineWavFile() {
        // init engine
        Engine.cancelShutdown()
        OfficialExtensions.initForTests()

        // create file
        val file = FileFileRef.createTempFile("audio", "wav")
        file.deleteOnExit()

        // fill with procedural audio
        val duration = 0.2
        createWavFile(file, duration)

        // play
        val (left, right) = loadWavFile(file, duration).waitFor()!!
        val numSamples = numSamples(duration)
        assertEquals(numSamples, left.size)
        assertEquals(numSamples, right.size)
        for (i in 0 until numSamples) {
            assertEquals(waveForm(i), left[i])
            assertEquals(waveForm(i + 10), right[i])
        }
    }

    fun loadWavFile(src: FileReference, duration: Double): Promise<AudioData> {
        return AudioFXCache.getBuffer(src, 0.0, duration, numSamples(duration), LoopingState.PLAY_ONCE)
    }

    fun createWavFile(dst: FileReference, duration: Double) {
        createStereoAudioFile(dst, duration, ::waveForm) { waveForm(it + 10) }
    }

    companion object {

        val sampleRate = playbackSampleRate.toFloat()
        val sampleSizeBits = 16
        val numChannels = 2
        val scale = (1 shl 15).toFloat()
        val signed = true
        val bigEndian = true

        fun numSamples(duration: Double): Int = (duration * sampleRate).toInt()

        fun createStereoAudioFile(
            dst: FileReference, duration: Double,
            leftChannel: (Int) -> Short,
            rightChannel: (Int) -> Short
        ) {
            val numSamples = numSamples(duration)
            val audioFormat = AudioFormat(sampleRate, sampleSizeBits, numChannels, signed, bigEndian)
            val bos = ByteArrayOutputStream(numSamples * 4)
            DataOutputStream(bos).use { dos ->
                for (i in 0 until numSamples) {
                    dos.writeShort(leftChannel(i).toInt())
                    dos.writeShort(rightChannel(i).toInt())
                }
            }
            val bis = ByteArrayInputStream(bos.toByteArray())
            val ais = AudioInputStream(bis, audioFormat, numSamples.toLong())
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, (dst as FileFileRef).file)
            dst.invalidate()
        }
    }
}