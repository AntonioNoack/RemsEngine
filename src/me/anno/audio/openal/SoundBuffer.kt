package me.anno.audio.openal

import me.anno.audio.openal.AudioManager.openALSession
import me.anno.audio.streams.AudioStream.Companion.bufferPool
import me.anno.cache.data.ICacheData
import me.anno.io.files.FileReference
import me.anno.utils.pooling.ByteBufferPool
import org.lwjgl.openal.AL10.*
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.newdawn.slick.openal.WaveData
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.*

class SoundBuffer() : ICacheData {

    var pointer = -1
    var session = -1

    var data0: ByteBuffer? = null
    var data: ShortBuffer? = null

    var sampleRate = 0

    fun ensurePointer() {
        if (pointer < 0 || session != openALSession) {
            pointer = alGenBuffers()
            session = openALSession
        }
        if (pointer < 0) throw OutOfMemoryError("Failed to create OpenAL buffer")
    }

    fun ensureData() {
        val data = data ?: throw IllegalStateException("Missing audio data")
        ensurePointer()
        ALBase.check()
        alBufferData(pointer, AL_FORMAT_STEREO16, data, sampleRate)
        ALBase.check()
        bufferPool.returnBuffer(data0)
        this.data0 = null
        this.data = null
    }

    constructor(file: FileReference) : this() {
        load(file)
    }

    constructor(waveData: WaveData) : this() {
        loadWAV(waveData)
    }

    fun loadRawStereo16(data: ShortBuffer, data0: ByteBuffer, sampleRate: Int) {
        this.data = data
        this.data0 = data0
        this.sampleRate = sampleRate
    }

    fun loadWAV(waveData: WaveData) {
        ensurePointer()
        data = waveData.data.asShortBuffer()
        alBufferData(pointer, waveData.format, waveData.data, waveData.sampleRate)
        waveData.dispose() // probably not required...
        ALBase.check()
    }

    fun loadOGG(file: FileReference) {
        STBVorbisInfo.malloc().use { info ->
            val pcm = readVorbis(file, info)
            val format = if (info.channels() == 1) AL_FORMAT_MONO16 else AL_FORMAT_STEREO16
            ensurePointer()
            alBufferData(pointer, format, pcm, info.sample_rate())
            ALBase.check()
        }
    }

    fun load(file: FileReference) {
        val name = file.name
        when (val ending = name.split('.').last().lowercase(Locale.getDefault())) {
            "ogg" -> loadOGG(file)
            "wav" -> loadWAV(WaveData.create(file.inputStream())!!)
            else -> throw RuntimeException("Unknown audio format $ending!")
        }
    }

    private fun readVorbis(file: FileReference, info: STBVorbisInfo): ShortBuffer {
        MemoryStack.stackPush().use { stack ->
            val rawBytes = ioResourceToByteBuffer(file)
            val error = stack.mallocInt(1)
            val decoder = stb_vorbis_open_memory(rawBytes, error, null)
            ALBase.check()
            if (decoder == 0L) {
                bufferPool.returnBuffer(rawBytes)
                throw RuntimeException("Failed to open Ogg Vorbis file. Error: " + error[0])
            }
            stb_vorbis_get_info(decoder, info)
            val channels = info.channels()
            val lengthSamples = stb_vorbis_stream_length_in_samples(decoder)
            val pcm = bufferPool[lengthSamples * 2, false, true].asShortBuffer()
            this.data = pcm
            pcm.limit(stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm) * channels)
            stb_vorbis_close(decoder)
            bufferPool.returnBuffer(rawBytes)
            return pcm
        }
    }

    private fun ioResourceToByteBuffer(file: FileReference): ByteBuffer {
        val bytes = file.readBytes()
        val buffer = ByteBufferPool
            .allocateDirect(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }

    override fun destroy() {
        if (pointer > -1 && session == openALSession) {
            alDeleteBuffers(pointer)
            pointer = -1
        }
        if (data != null) {
            bufferPool.returnBuffer(data0)
            data0 = null
            data = null
        }
    }

}