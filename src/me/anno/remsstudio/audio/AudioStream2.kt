package me.anno.remsstudio.audio

import me.anno.animation.LoopingState
import me.anno.audio.AudioStream
import me.anno.io.files.FileReference
import me.anno.remsstudio.audio.effects.Time
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Camera
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGMetadata.Companion.getMeta

// only play once, then destroy; it makes things easier
// (on user input and when finally rendering only)

// done viewing the audio levels is more important than effects
// done especially editing the audio levels is important (amplitude)


// idk does not work, if the buffers aren't filled fast enough -> always fill them fast enough...
// idk or restart playing...

/**
 * todo audio effects:
 * done better echoing ;)
 * todo velocity frequency change
 * done pitch
 * todo losing high frequencies in the distance
 * done audio becoming quiet in the distance
 * */
abstract class AudioStream2(
    file: FileReference,
    repeat: LoopingState,
    startIndex: Long,
    meta: FFMPEGMetadata,
    val source: Audio,
    val destination: Camera,
    speed: Double,
    playbackSampleRate: Int = 48000
) : AudioStream(file, repeat, startIndex, meta, speed, playbackSampleRate) {

    constructor(audio: Audio, speed: Double, globalTime: Double, playbackSampleRate: Int, listener: Camera) :
            this(
                audio.file,
                audio.isLooping.value,
                getIndex(globalTime, speed, playbackSampleRate),
                getMeta(audio.file, false)!!,
                audio,
                listener,
                speed,
                playbackSampleRate
            )

    init {
        source.pipeline.audio = source
    }

    fun getTime(index: Long): Time = getTime(getTimeD(index))
    private fun getTime(globalTime: Double): Time = Time(globalToLocalTime(globalTime), globalTime)

    override fun getBuffer(bufferIndex: Long): Pair<FloatArray, FloatArray> {
        return AudioFXCache2.getBuffer(bufferIndex, this, false)!!
    }

    // todo is this correct with the speed?
    private fun globalToLocalTime(time: Double) = source.getGlobalTime(time * speed)

}