package me.anno.video

import me.anno.audio.AudioStream
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Camera
import me.anno.utils.Sleep.waitUntil
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicInteger

class BufferStream(
    val audio: Audio, sampleRate: Int,
    listener: Camera
) :
    AudioStream(audio, 1.0, 0.0, sampleRate, listener) {

    private val filledBuffers = ArrayList<ShortBuffer?>()
    private val gettingIndex = AtomicInteger()

    // todo whoever calls this function must return the buffer!!
    fun getAndReplace(): ShortBuffer {
        val index = gettingIndex.getAndIncrement()
        waitUntil(true) { filledBuffers.size > index }
        return filledBuffers.set(index, null)!!
    }

    override fun onBufferFilled(stereoBuffer: ShortBuffer, sb0: ByteBuffer, bufferIndex: Long, session: Int): Boolean {
        filledBuffers.add(stereoBuffer)
        return false
    }

}