package me.anno.audio.streams

import me.anno.animation.LoopingState
import me.anno.audio.AudioPools.SAPool
import me.anno.audio.AudioReadable
import me.anno.audio.openal.SoundBuffer
import me.anno.audio.AudioCache
import me.anno.audio.AudioSliceKey
import me.anno.io.files.FileReference
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.structures.tuples.ShortPair
import me.anno.video.ffmpeg.MediaMetadata
import me.anno.video.ffmpeg.FFMPEGStream.Companion.getAudioSequence
import org.lwjgl.openal.AL10
import kotlin.math.max
import kotlin.math.min

class AudioStreamRaw(
    val file: FileReference,
    val repeat: LoopingState,
    val meta: MediaMetadata,
    val time0: Double, val time1: Double
) : StereoShortStream {

    // todo if out of bounds, and not recoverable, just stop

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {

        // 1024 (48Hz .. 48kHz) or 2048? (24Hz .. 48kHz)
        var bufferSize = 4096

        // should be set by the engine depending on the OS
        // could be overridden manually, e.g. to get a 8kHz vibe;
        // if you do that, consider overriding bufferSize as well, so the audio could be adjusted faster if needed (idk yet)
        var playbackSampleRate = 48000
        val ffmpegSliceSampleDuration = 30.0 // seconds, 30s of music

        inline fun averageSamples(
            mni: Double, mxi: Double,
            s0: ShortPair, s1: ShortPair, s2: ShortPair, dst: ShortPair,
            getMaxAmplitudesSync: (i: Long, s: ShortPair) -> Unit
        ) {

            val mnI = mni.toLong()
            val mxI = mxi.toLong()

            if (mnI == mxI) {
                // time is roughly standing still
                // from the same index, so 50:50
                getMaxAmplitudesSync(mnI, s0)
                getMaxAmplitudesSync(mnI + 1, s1)
                val fract = fract(mni + mxi).toFloat()
                dst.set(
                    mix(s0.first, s1.first, fract),
                    mix(s0.second, s1.second, fract)
                )
            } else {
                // time is changing
                // sampling from all values
                // (slow motion sound effects)
                getMaxAmplitudesSync(mnI, s0)
                getMaxAmplitudesSync(mxI, s1)
                val f0i = 1f - (mni - mnI).toFloat() // x.2f -> 0.8f
                val f1i = (mxi - mxI).toFloat() // x.2f -> 0.2f
                var b0 = s0.first * f0i + s1.first * f1i
                var b1 = s0.second * f0i + s1.second * f1i
                for (index in mnI + 1 until mxI) {
                    getMaxAmplitudesSync(index, s2)
                    b0 += s2.first
                    b1 += s2.second
                }
                val dt = (mxi - mni).toFloat()
                // average the values over the time span
                dst.set(b0 / dt, b1 / dt)
            }
        }
    }

    val sampleRate = meta.audioSampleRate
    val sampleCount = meta.audioSampleCount

    val ffmpegSliceSampleCount = (sampleRate * ffmpegSliceSampleDuration).toInt()

    private var lastSliceIndex = Long.MAX_VALUE
    private var lastSoundBuffer: SoundBuffer? = null

    fun getAmplitudeSync(index0: Long, shortPair: ShortPair) {

        val maxSampleIndex = sampleCount
        val repeat = repeat

        if (index0 < 0 || (repeat === LoopingState.PLAY_ONCE && index0 >= maxSampleIndex)) {

            shortPair.set(0, 0)

        } else {

            val index = repeat[index0, maxSampleIndex]

            if (file is AudioReadable) {

                if (file.prefersBufferedSampling) {
                    // we use buffer-length slices by our internal generation functions,
                    // because they probably have low overhead
                    val sliceSampleCount = ((time1 - time0) * sampleRate).toInt()
                    val index0i = (time0 * sampleRate).toLong()
                    val soundBuffer = if (lastSoundBuffer == null) {
                        val soundBuffer = file.getBuffer(time0, time1, sampleRate)
                        lastSoundBuffer = soundBuffer
                        soundBuffer
                    } else lastSoundBuffer!!
                    val data = soundBuffer.data!!
                    val localIndex = clamp((index - index0i).toInt(), 0, sliceSampleCount - 1)
                    if (soundBuffer.format == AL10.AL_FORMAT_STEREO16) {
                        val arrayIndex0 = localIndex * 2 // for stereo
                        shortPair.set(data[arrayIndex0], data[arrayIndex0 + 1])
                    } else {
                        val v = data[localIndex] // mono
                        shortPair.set(v, v)
                    }
                } else {
                    // easiest case: file wants to be sampled one-by-one
                    val time = index.toDouble() / sampleRate
                    if (file.channels == 2) {
                        shortPair.set(file.sample(time, 0), file.sample(time, 1))
                    } else {
                        val v = file.sample(time, 0)
                        shortPair.set(v, v)
                    }
                }
            } else {

                // we use long slices by FFMPEG, because calling FFMPEG has a relatively high overhead
                val sliceSampleCount = ffmpegSliceSampleCount
                val sliceIndex = index / sliceSampleCount
                val soundBuffer = if (sliceIndex == lastSliceIndex) {
                    lastSoundBuffer!!
                } else {
                    val file = file
                    val sliceDuration = ffmpegSliceSampleDuration
                    val key = AudioSliceKey(file, sliceIndex)
                    val timeout = (sliceDuration * 2 * 1000).toLong()
                    val sliceTime = sliceIndex * sliceDuration
                    val soundBuffer = AudioCache.getEntry(key, timeout, false) {
                        val sequence = getAudioSequence(file, sliceTime, sliceDuration, sampleRate)
                        waitUntilDefined(true) {
                            if (sequence.isEmpty) SoundBuffer(0)
                            else sequence.soundBuffer
                        }
                    } as SoundBuffer
                    lastSoundBuffer = soundBuffer
                    lastSliceIndex = sliceIndex
                    soundBuffer
                }

                val data = soundBuffer.data!!
                val localIndex = (index % sliceSampleCount).toInt()
                if (soundBuffer.format == AL10.AL_FORMAT_STEREO16) {
                    val arrayIndex0 = localIndex * 2 // for stereo
                    shortPair.set(data[arrayIndex0], data[arrayIndex0 + 1])
                } else {
                    val v = data[localIndex] // mono
                    shortPair.set(v, v)
                }
            }
        }

    }

    override fun getBuffer(
        bufferSize: Int,
        time0: Double,
        time1: Double,
    ): Pair<ShortArray, ShortArray> {

        val index0 = sampleRate * time0
        val index1 = sampleRate * time1

        val leftBuffer = SAPool[bufferSize, true, true]
        val rightBuffer = SAPool[bufferSize, true, true]

        val s0 = ShortPair()
        val s1 = ShortPair()
        val s2 = ShortPair()
        val dst = ShortPair()

        var indexI = index0
        for (sampleIndex in 0 until bufferSize) {

            val indexJ = mix(index0, index1, (sampleIndex + 1).toDouble() / bufferSize)

            // average values from index0 to index1
            val mni = min(indexI, indexJ)
            val mxi = max(indexI, indexJ)

            averageSamples(mni, mxi, s0, s1, s2, dst, this::getAmplitudeSync)

            // write the data
            leftBuffer[sampleIndex] = dst.first
            rightBuffer[sampleIndex] = dst.second

            indexI = indexJ

        }

        return leftBuffer to rightBuffer

    }

}