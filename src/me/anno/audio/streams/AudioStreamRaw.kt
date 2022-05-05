package me.anno.audio.streams

import me.anno.animation.LoopingState
import me.anno.audio.AudioPools.FAPool
import me.anno.audio.openal.SoundBuffer
import me.anno.cache.instances.AudioCache
import me.anno.cache.keys.AudioSliceKey
import me.anno.io.files.FileReference
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.mix
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGStream.Companion.getAudioSequence
import kotlin.math.max
import kotlin.math.min

class AudioStreamRaw(
    val file: FileReference,
    val repeat: LoopingState,
    val meta: FFMPEGMetadata
) : StereoFloatStream {

    // todo if out of bounds, and not recoverable, just stop

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {

        // 1024 (48Hz .. 48kHz) or 2048? (24Hz .. 48kHz)
        val bufferSize = 4096
        val ffmpegSliceSampleDuration = 30.0 // seconds, 10s of music

        inline fun averageSamples(
            mni: Double, mxi: Double, s0: ShortPair, s1: ShortPair, s2: ShortPair,
            dst: FloatPair,
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
                    mix(s0.left, s1.left, fract),
                    mix(s0.right, s1.right, fract)
                )
            } else {
                // time is changing
                // sampling from all values
                // (slow motion sound effects)
                getMaxAmplitudesSync(mnI, s0)
                getMaxAmplitudesSync(mxI, s1)
                val f0i = 1f - (mni - mnI).toFloat() // x.2f -> 0.8f
                val f1i = (mxi - mxI).toFloat() // x.2f -> 0.2f
                var b0 = s0.left * f0i + s1.left * f1i
                var b1 = s0.right * f0i + s1.right * f1i
                for (index in mnI + 1 until mxI) {
                    getMaxAmplitudesSync(index, s2)
                    b0 += s2.left
                    b1 += s2.right
                }
                val dt = (mxi - mni).toFloat()
                // average the values over the time span
                dst.set(b0 / dt, b1 / dt)
            }

        }

    }

    val ffmpegSampleRate = meta.audioSampleRate
    val maxSampleIndex = meta.audioSampleCount

    val ffmpegSliceSampleCount = (ffmpegSampleRate * ffmpegSliceSampleDuration).toInt()

    private var lastSliceIndex = Long.MAX_VALUE
    private var lastSoundBuffer: SoundBuffer? = null

    fun getAmplitudeSync(index0: Long, shortPair: ShortPair) {

        val maxSampleIndex = maxSampleIndex
        val repeat = repeat

        if (index0 < 0 || (repeat === LoopingState.PLAY_ONCE && index0 >= maxSampleIndex)) {

            shortPair.set(0, 0)

        } else {

            val index = repeat[index0, maxSampleIndex]

            val ffmpegSliceSampleCount = ffmpegSliceSampleCount
            val sliceIndex = index / ffmpegSliceSampleCount
            val soundBuffer: SoundBuffer = if (sliceIndex == lastSliceIndex) {
                lastSoundBuffer!!
            } else {
                val file = file
                val ffmpegSliceSampleDuration = ffmpegSliceSampleDuration
                val key = AudioSliceKey(file, sliceIndex)
                val timeout = (ffmpegSliceSampleDuration * 2 * 1000).toLong()
                val sliceTime = sliceIndex * ffmpegSliceSampleDuration
                val soundBuffer = AudioCache.getEntry(key, timeout, false) {
                    val sequence = getAudioSequence(file, sliceTime, ffmpegSliceSampleDuration, ffmpegSampleRate)
                    waitUntilDefined(true) { sequence.soundBuffer }
                } as SoundBuffer
                lastSoundBuffer = soundBuffer
                lastSliceIndex = sliceIndex
                soundBuffer
            }

            val data = soundBuffer.data!!
            val localIndex = (index % ffmpegSliceSampleCount).toInt()
            val arrayIndex0 = localIndex * 2 // for stereo

            shortPair.set(data[arrayIndex0], data[arrayIndex0 + 1])

        }

    }

    override fun getBuffer(
        bufferSize: Int,
        time0: Double,
        time1: Double
    ): Pair<FloatArray, FloatArray> {

        val index0 = ffmpegSampleRate * time0
        val index1 = ffmpegSampleRate * time1

        val leftBuffer = FAPool[bufferSize, true, true]
        val rightBuffer = FAPool[bufferSize, true, true]

        val s0 = ShortPair()
        val s1 = ShortPair()
        val s2 = ShortPair()
        val dst = FloatPair()

        var indexI = index0
        for (sampleIndex in 0 until bufferSize) {

            val indexJ = mix(index0, index1, sampleIndex.toDouble() / bufferSize)

            // average values from index0 to index1
            val mni = min(indexI, indexJ)
            val mxi = max(indexI, indexJ)

            averageSamples(mni, mxi, s0, s1, s2, dst, this::getAmplitudeSync)

            // write the data
            leftBuffer[sampleIndex] = dst.left
            rightBuffer[sampleIndex] = dst.right

            indexI = indexJ

        }

        return leftBuffer to rightBuffer

    }

}