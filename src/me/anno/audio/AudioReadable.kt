package me.anno.audio

import me.anno.audio.AudioCache.playbackSampleRate

interface AudioReadable {

    // these are mainly used as metadata
    val channels: Int get() = 1
    val sampleCount: Long get() = 10_000_000_000L
    val sampleRate: Int get() = playbackSampleRate
    val duration: Double get() = sampleCount.toDouble() / sampleRate

    fun sample(time: Double, channel: Int): Short
}