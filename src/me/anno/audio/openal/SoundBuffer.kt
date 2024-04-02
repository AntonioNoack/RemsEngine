package me.anno.audio.openal

import me.anno.audio.openal.AudioManager.openALSession
import me.anno.audio.streams.AudioStream.Companion.bufferPool
import me.anno.cache.ICacheData
import org.lwjgl.openal.AL11.AL_FORMAT_MONO16
import org.lwjgl.openal.AL11.AL_FORMAT_STEREO16
import org.lwjgl.openal.AL11.alBufferData
import org.lwjgl.openal.AL11.alDeleteBuffers
import org.lwjgl.openal.AL11.alGenBuffers
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class SoundBuffer : ICacheData {

    private var session = -1
    var pointer = 0
        private set

    private var data0: ByteBuffer? = null
    var data: ShortBuffer? = null
        private set

    private var sampleRate = 0
    private var format = AL_FORMAT_MONO16

    val isStereo get() = format == AL_FORMAT_STEREO16

    fun ensurePointer() {
        if (pointer == 0 || session != openALSession) {
            pointer = alGenBuffers()
            session = openALSession
        }
        if (pointer == 0) throw OutOfMemoryError("Failed to create OpenAL buffer")
    }

    fun ensureData() {
        val data = data ?: throw IllegalStateException("Missing audio data")
        ensurePointer()
        ALBase.check()
        alBufferData(pointer, format, data, sampleRate)
        ALBase.check()
        bufferPool.returnBuffer(data0)
        this.data0 = null
        this.data = null
    }

    fun loadRaw16(data: ShortBuffer, data0: ByteBuffer, sampleRate: Int, format: Int) {
        this.data = data
        this.data0 = data0
        this.sampleRate = sampleRate
        this.format = format
    }

    override fun destroy() {
        if (pointer != 0 && session == openALSession) {
            alDeleteBuffers(pointer)
            pointer = 0
        }
        if (data0 != null) {
            bufferPool.returnBuffer(data0)
            data0 = null
            data = null
        }
    }
}