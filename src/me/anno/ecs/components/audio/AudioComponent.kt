package me.anno.ecs.components.audio

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.audio.AudioFXCache
import me.anno.audio.AudioReadable
import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import kotlin.math.abs
import kotlin.math.ceil

/**
 * plays audio
 * */
class AudioComponent : AudioComponentBase() {

    @Docs("Where the audio file to be used is located")
    var source: FileReference = InvalidRef

    // most tracks are short, so keep them in memory by default
    @Docs("Keeps the track in memory, so it can be started without delay")
    var keepInMemory = true

    @Docs("Will start the audio as soon as it's available")
    var autoStart = false

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is AudioComponent) return
        dst.source = source
        dst.keepInMemory = keepInMemory
    }

    private fun keepInMemory() {
        // calculate number of buffers
        val meta = MediaMetadata.getMeta(source).value ?: return
        val duration = meta.duration
        val numBuffers = ceil(duration * meta.audioSampleRate / bufferSize).toInt()
        for (i in 0 until numBuffers) {
            keepInMemory(meta, i)
        }
    }

    private fun keepInMemory(meta: MediaMetadata, index: Int): Boolean {
        // keep in memory
        val time0 = index.toDouble() * bufferSize / meta.audioSampleRate
        val time1 = (index + 1).toDouble() * bufferSize / meta.audioSampleRate
        return AudioFXCache.getBuffer(
            source, time0, time1, bufferSize, when (playMode) {
                PlayMode.LOOP -> LoopingState.PLAY_LOOP
                else -> LoopingState.PLAY_ONCE
            }
        ).value != null
    }

    fun isFullyLoaded(): Boolean {
        val meta = MediaMetadata.getMeta(source).value ?: return false
        val duration = meta.duration
        val numBuffers = ceil(duration * meta.audioSampleRate / bufferSize).toInt()
        for (i in 0 until numBuffers) {
            if (!keepInMemory(meta, i)) return false
        }
        return true
    }

    override fun onUpdate() {
        super.onUpdate()
        if (keepInMemory && source !is AudioReadable) {
            keepInMemory()
        }
        if (autoStart && !isPlaying && abs(startTime - Time.nanoTime) > 1e9) {
            start()
        }
    }
}