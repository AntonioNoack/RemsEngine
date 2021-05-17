package me.anno.audio.openal

import me.anno.audio.*
import me.anno.io.FileReference
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.modes.LoopingState
import me.anno.video.AudioCreator.Companion.playbackSampleRate
import me.anno.video.FFMPEGMetadata
import org.apache.logging.log4j.LogManager
import org.lwjgl.openal.AL10.*
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

// only play once, then destroy; it makes things easier
// (on user input and when finally rendering only)

// todo destroy OpenAL context when stopping playing, and recreate when starting
// todo to fix "AL lib: (EE) ALCwasapiPlayback_mixerProc: Failed to get padding: 0x88890004"

class AudioStreamOpenAL(
    file: FileReference,
    repeat: LoopingState,
    var startTime: Double,
    meta: FFMPEGMetadata,
    sender: Audio,
    listener: Camera,
    speed: Double
) : AudioStream(file, repeat, getIndex(startTime, speed, playbackSampleRate), meta, sender, listener, speed) {

    constructor(audio: Audio, speed: Double, globalTime: Double, listener: Camera) :
            this(
                audio.file, audio.isLooping.value, globalTime,
                FFMPEGMetadata.getMeta(audio.file, false)!!,
                audio, listener, speed
            )

    companion object {
        private val LOGGER = LogManager.getLogger(AudioStreamOpenAL::class)
    }

    val startTime0 = startTime

    var startTimeNanos = 0L
    var realStartTimeNanos = 0L
    var alSource = SoundSource(false, true)

    var queued = AtomicLong()
    var processed = 0

    fun checkProcessed() {
        processed = alGetSourcei(alSource.sourcePtr, AL_BUFFERS_PROCESSED)
        ALBase.check()
    }

    fun start() {
        if (!isPlaying) {
            isPlaying = true
            startTimeNanos = System.nanoTime()
            realStartTimeNanos = startTimeNanos
            waitForRequiredBuffers()
        } else throw RuntimeException()
    }

    fun stop() {
        if (!isPlaying) return
        isPlaying = false
        alSource.stop()
        alSource.destroy()
        ALBase.check()
        // ALBase.check()
        // somehow crashes..., buffers can't be reused either (without error)
        // buffers.toSet().forEach { it.destroy() }
        // ALBase.check()
    }

    val cachedBuffers = 10

    fun checkSession() {
        if (alSource.checkSessionWasReset()) {
            // reset all progress
            val time = System.nanoTime()
            // find start time and start index
            val deltaTime = (time - realStartTimeNanos) * 1e-9
            startTime = startTime0 + deltaTime * speed
            startIndex = getIndex(startTime, speed, playbackSampleRate)
            hadFirstBuffer = false
            queued.set(0)
            processed
            startTimeNanos = time
        }
    }

    fun waitForRequiredBuffers() {
        if (!isPlaying) return
        checkSession()
        val queued = queued.get()
        if (!isWaitingForBuffer.get() && queued > 0) checkProcessed()
        // keep 2 on reserve
        if (queued < processed + cachedBuffers && !isWaitingForBuffer.get()) {
            // request a buffer
            // only one at a time
            val index = this.queued.getAndIncrement()
            // loading $index...
            requestNextBuffer(index)
        }
        if (isPlaying) {
            AudioTasks.addNextTask(1) {
                waitForRequiredBuffers()
                ALBase.check()
            }
        }
    }

    var hadFirstBuffer = false

    override fun onBufferFilled(stereoBuffer: ShortBuffer, bufferIndex: Long) {
        if (!isPlaying) return
        AudioTasks.addTask(10) {
            if (isPlaying) {

                checkSession()

                ALBase.check()
                val soundBuffer = SoundBuffer()
                ALBase.check()

                // wait until we have enough data to be played back
                // then it needs to work like this, because that's already perfect:

                // load audio continuously the whole time, so we have it, when it's required

                if (!hadFirstBuffer) {

                    val dt = max(0f, (System.nanoTime() - startTimeNanos) * 1e-9f)
                    LOGGER.info("Skipping first ${dt}s")
                    // 10s slices -> 2.6s
                    // 1s slices -> 0.55s

                    val startOffset = getFraction(startTime, speed, playbackSampleRate)
                    val samples = dt * playbackSampleRate + startOffset
                    val capacity = stereoBuffer.capacity()
                    val targetIndex = samples.toInt() * 2 - bufferIndex * capacity
                    if (capacity > targetIndex + 256) {

                        LOGGER.info("Skipping $targetIndex/$capacity")
                        stereoBuffer.position(targetIndex.toInt())

                        hadFirstBuffer = true

                    } else {
                        // else delayed, but we have no alternative
                        LOGGER.warn("Skipping first buffer completely")
                        return@addTask
                    }
                }

                soundBuffer.loadRawStereo16(stereoBuffer, playbackSampleRate)
                soundBuffer.ensureData()
                buffers.add(soundBuffer)
                ALBase.check()

                alSourceQueueBuffers(alSource.sourcePtr, soundBuffer.buffer)
                ALBase.check()

                alSource.play()
                ALBase.check()

                // time += openALSliceDuration
                isWaitingForBuffer.set(false)
                ALBase.check()

            }
        }
    }

}