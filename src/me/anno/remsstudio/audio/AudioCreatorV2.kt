package me.anno.remsstudio.audio

import me.anno.audio.streams.AudioStream
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.objects.Transform
import me.anno.video.AudioCreator

class AudioCreatorV2(
    val scene: Transform,
    val camera: Camera,
    val audioSources: List<Audio>,
    durationSeconds: Double,
    sampleRate: Int,
) : AudioCreator(durationSeconds, sampleRate) {

    override fun hasStreams(): Boolean {
        return audioSources.isNotEmpty()
    }

    override fun createStreams(): List<AudioStream> {
        return audioSources.map { AudioFileStream2(it, 1.0, 0.0, sampleRate, camera) }
    }

}