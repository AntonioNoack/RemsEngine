package me.anno.audio.streams

import me.anno.animation.LoopingState
import me.anno.audio.AudioFXCache
import me.anno.io.files.FileReference
import me.anno.video.ffmpeg.FFMPEGMetadata

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
abstract class AudioFileStream(
    val file: FileReference,
    val repeat: LoopingState,
    var startIndex: Long,
    val meta: FFMPEGMetadata,
    speed: Double,
    playbackSampleRate: Int = 48000
) : AudioStream(speed, playbackSampleRate) {

    override fun getBuffer(bufferIndex: Long): Pair<FloatArray, FloatArray> {
        return AudioFXCache.getBuffer(bufferIndex, this, false)!!
    }

}