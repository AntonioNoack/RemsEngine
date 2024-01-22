package me.anno.audio

import me.anno.audio.AudioCache.playbackSampleRate
import me.anno.audio.openal.SoundBuffer
import me.anno.audio.streams.AudioStream
import org.lwjgl.openal.AL11

interface AudioReadable {

    // these are mainly used as metadata
    val channels: Int get() = 1
    val sampleCount: Long get() = 10_000_000_000L
    val sampleRate: Int get() = playbackSampleRate
    val duration: Double get() = sampleCount.toDouble() / sampleRate

    val prefersBufferedSampling: Boolean get() = false

    // actual audio generation function
    fun getBuffer(start: Double, duration: Double, sampleRate: Int): SoundBuffer {

        // generate buffer :)
        val buffer = SoundBuffer()
        val bufferLength = (duration * sampleRate).toInt()
        buffer.format = AL11.AL_FORMAT_MONO16
        val bytes = AudioStream.bufferPool[bufferLength * 2 * channels, false, true]
        val shorts = bytes.asShortBuffer()
        buffer.data0 = bytes
        buffer.data = shorts

        val dt = 1.0 / sampleRate
        var time = start

        when (channels) {
            1 -> {
                for (i in 0 until bufferLength) {
                    shorts.put(sample(time, 0))
                    time += dt
                }
            }
            2 -> {
                for (i in 0 until bufferLength) {
                    shorts.put(sample(time, 0))
                    shorts.put(sample(time, 1))
                    time += dt
                }
            }
            else -> {
                for (i in 0 until bufferLength) {
                    for (j in 0 until channels) {
                        shorts.put(sample(time, j))
                    }
                    time += dt
                }
            }
        }

        shorts.flip()
        return buffer

    }

    fun sample(time: Double, channel: Int): Short

}