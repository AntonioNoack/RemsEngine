package me.anno.video

import me.anno.audio.AudioStream
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.utils.LOGGER
import me.anno.utils.Sleep.sleepABit
import me.anno.utils.Sleep.waitUntil
import java.nio.BufferOverflowException
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicInteger

class BufferStream(
    val audio: Audio, sampleRate: Int,
    listener: Camera
) :
    AudioStream(audio, 1.0, 0.0, sampleRate, listener) {

    private val filledBuffers = ArrayList<ShortBuffer?>()
    private val gettingIndex = AtomicInteger()

    fun getAndReplace(): ShortBuffer {
        val index = gettingIndex.getAndIncrement()
        waitUntil { filledBuffers.size > index }
        return filledBuffers.set(index, null)!!
    }

    override fun onBufferFilled(stereoBuffer: ShortBuffer, bufferIndex: Long) {
        filledBuffers.add(stereoBuffer)
    }

}