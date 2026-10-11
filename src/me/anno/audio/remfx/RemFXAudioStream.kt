package me.anno.audio.remfx

import me.anno.audio.AudioData
import me.anno.audio.AudioPools
import me.anno.audio.openal.ALBase
import me.anno.audio.openal.ALBase.isALThread
import me.anno.audio.openal.AudioTasks
import me.anno.audio.openal.AudioTasks.addAudioTask
import me.anno.audio.openal.SoundBuffer
import me.anno.audio.openal.SoundSource
import me.anno.audio.streams.AudioStream
import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.cache.Promise
import org.lwjgl.openal.AL10.*
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicLong

class RemFXAudioStream(val player: RemFXPlayer) :
    AudioStream(
        speed = 1.0, playbackSampleRate = 48000,
        left = true, center = false, right = true
    ) {

    override fun getBuffer(bufferIndex: Long): Promise<AudioData> {

        val left = AudioPools.SAPool[bufferSize, false, true]
        val right = AudioPools.SAPool[bufferSize, false, true]

        player.fill(bufferIndex, left, right)

        val data = AudioData(left, right)
        return Promise(data)
    }

    val alSource = SoundSource(loop = false, relativePositionsToListener = false)

    var queued = AtomicLong()
    var processed = 0

    val buffers = ArrayList<SoundBuffer>()

    fun checkProcessed() {
        processed = alGetSourcei(alSource.ensurePointer(), AL_BUFFERS_PROCESSED)
        ALBase.check()
    }

    fun start() {
        if (isPlaying) return
        check(isALThread())

        isPlaying = true
        waitForRequiredBuffers()
    }

    fun stop() {
        if (!isPlaying) return
        check(isALThread())

        isPlaying = false
        alSource.stop()
        alSource.destroy()
        ALBase.check()
        for (buffer in buffers) {
            buffer.destroy()
        }
    }

    val cachedBuffers = 10

    fun checkSession() {
        if (alSource.checkSessionWasReset()) {
            // find start time and start index
            queued.set(0)
        }
    }

    fun waitForRequiredBuffers() {
        if (!isPlaying) return
        checkSession()
        val queued = queued.get()
        if (!isWaitingForBuffer && queued > 0) checkProcessed()
        // keep 2 on reserve
        if (queued < processed + cachedBuffers && !isWaitingForBuffer) {
            // request a buffer
            // only one at a time
            val index = this.queued.getAndIncrement()
            // loading $index...
            requestNextBuffer(index, alSource.session)
        }
        if (isPlaying) {
            AudioTasks.addNextAudioTask("wait", 1) {
                waitForRequiredBuffers()
                ALBase.check()
            }
        }
    }

    override fun onBufferFilled(
        stereoBuffer: ShortBuffer, byteBuffer: ByteBuffer,
        bufferIndex: Long, session: Int
    ): Boolean {

        if (!isPlaying) return true

        addAudioTask("buffer filling", 10) {
            if (isPlaying) {

                checkSession()

                // wait until we have enough data to be played back
                // then it needs to work like this, because that's already perfect:

                // load audio continuously the whole time, so we have it, when it's required

                ALBase.check()
                val soundBuffer = SoundBuffer()
                ALBase.check()
                soundBuffer.loadRaw16(stereoBuffer, byteBuffer, playbackSampleRate, AL_FORMAT_STEREO16)
                soundBuffer.ensureData()
                buffers.add(soundBuffer)
                ALBase.check()

                alSourceQueueBuffers(alSource.ensurePointer(), soundBuffer.pointer)
                ALBase.check()

                alSource.play()
                ALBase.check()

                isWaitingForBuffer = false
                ALBase.check()
            }
        }

        return false
    }
}