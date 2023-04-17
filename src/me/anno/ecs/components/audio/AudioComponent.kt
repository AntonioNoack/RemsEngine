package me.anno.ecs.components.audio

import me.anno.animation.LoopingState
import me.anno.audio.AudioFXCache
import me.anno.audio.streams.AudioStreamRaw.Companion.bufferSize
import me.anno.ecs.annotations.Docs
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.video.ffmpeg.FFMPEGMetadata
import kotlin.math.ceil

class AudioComponent : AudioComponentBase() {

    // todo autostart option

    var source: FileReference = InvalidRef

    // most tracks are short, so keep them in memory by default
    @Docs("Keeps the track in memory, so it can be started without delay")
    var keepInMemory = true

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as AudioComponent
        dst.source = source
        dst.keepInMemory = keepInMemory
    }

    private fun keepInMemory() {
        // calculate number of buffers
        val meta = FFMPEGMetadata.getMeta(source, true) ?: return
        val duration = meta.duration
        val numBuffers = ceil(duration * meta.audioSampleRate / bufferSize).toInt()
        for (i in 0 until numBuffers) {
            keepInMemory(meta, i)
        }
    }

    private fun keepInMemory(meta: FFMPEGMetadata, index: Int): Boolean {
        // keep in memory
        val time0 = index.toDouble() * bufferSize / meta.audioSampleRate
        val time1 = (index + 1).toDouble() * bufferSize / meta.audioSampleRate
        return AudioFXCache.getBuffer(
            source, time0, time1, bufferSize, when (playMode) {
                PlayMode.LOOP -> LoopingState.PLAY_LOOP
                else -> LoopingState.PLAY_ONCE
            }, false
        ) != null
    }

    fun isFullyLoaded(): Boolean {
        val meta = FFMPEGMetadata.getMeta(source, true) ?: return false
        val duration = meta.duration
        val numBuffers = ceil(duration * meta.audioSampleRate / bufferSize).toInt()
        for (i in 0 until numBuffers) {
            if (!keepInMemory(meta, i)) return false
        }
        return true
    }

    override fun onUpdate(): Int {
        super.onUpdate()
        if (keepInMemory) {
            keepInMemory()
        }
        return 1
    }

    override val className get() = "AudioComponent"

}