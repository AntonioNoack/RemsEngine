package me.anno.audio.streams

import me.anno.animation.LoopingState
import me.anno.audio.AudioData
import me.anno.audio.AudioFXCache
import me.anno.cache.AsyncCacheData
import me.anno.io.files.FileReference
import me.anno.io.MediaMetadata

/**
 * audio effects:
 * done better echoing ;)
 * done by OpenAL: velocity frequency change
 * done pitch
 * todo losing high frequencies in the distance
 * done audio becoming quiet in the distance
 * */
abstract class AudioFileStream(
    val file: FileReference,
    val repeat: LoopingState,
    var startIndex: Long,
    val meta: MediaMetadata,
    speed: Double,
    playbackSampleRate: Int = 48000,
    left: Boolean, center: Boolean, right: Boolean
) : AudioStream(speed, playbackSampleRate, left, center, right) {
    override fun getBuffer(bufferIndex: Long): AsyncCacheData<AudioData> {
        return AudioFXCache.getBuffer(bufferIndex, this)
    }
}