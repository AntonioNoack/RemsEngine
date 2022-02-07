package me.anno.audio

import me.anno.animation.LoopingState
import me.anno.audio.AudioPools.FAPool
import me.anno.audio.openal.SoundBuffer
import me.anno.cache.instances.AudioCache
import me.anno.cache.keys.AudioSliceKey
import me.anno.io.files.FileReference
import me.anno.maths.Maths.mix
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.hpc.ProcessingGroup
import me.anno.video.AudioCreator.Companion.playbackSampleRate
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGStream.Companion.getAudioSequence
import kotlin.math.max
import kotlin.math.min

class AudioStreamRaw(
    val file: FileReference,
    val repeat: LoopingState,
    val meta: FFMPEGMetadata
) {

    // todo if out of bounds, and not recoverable, just stop

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {
        // 1024 (48Hz .. 48kHz) or 2048? (24Hz .. 48kHz)
        val bufferSize = 4096
        val ffmpegSliceSampleDuration = 30.0 // seconds, 10s of music
    }

    val ffmpegSampleRate = meta.audioSampleRate
    val maxSampleIndex = meta.audioSampleCount

    val ffmpegSliceSampleCount = (ffmpegSampleRate * ffmpegSliceSampleDuration).toInt()

    class ShortPair(var left: Short, var right: Short) {

        constructor() : this(0, 0)

        fun set(left: Short, right: Short): ShortPair {
            this.left = left
            this.right = right
            return this
        }

    }

    private var lastSliceIndex = Long.MAX_VALUE
    private var lastSoundBuffer: SoundBuffer? = null

    private fun getMaxAmplitudesSync(index0: Long, shortPair: ShortPair): ShortPair {

        val maxSampleIndex = maxSampleIndex
        val repeat = repeat

        if (index0 < 0 || (repeat === LoopingState.PLAY_ONCE && index0 >= maxSampleIndex)) return shortPair.set(0, 0)
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

        return shortPair.set(data[arrayIndex0], data[arrayIndex0 + 1])

    }

    fun getBuffer(
        bufferSize: Int,
        time0: Double,
        time1: Double
    ): Pair<FloatArray, FloatArray> {

        val index0 = ffmpegSampleRate * time0
        val index1 = ffmpegSampleRate * time1

        val leftBuffer = FAPool[bufferSize, true]
        val rightBuffer = FAPool[bufferSize, true]

        val s0 = ShortPair()
        val s1 = ShortPair()
        val s2 = ShortPair()

        var indexI = index0
        for (sampleIndex in 0 until bufferSize) {

            val indexJ = mix(index0, index1, sampleIndex.toDouble() / bufferSize)

            // average values from index0 to index1
            val mni = min(indexI, indexJ)
            val mxi = max(indexI, indexJ)

            val mnI = mni.toLong()
            val mxI = mxi.toLong()

            var a0: Float
            var a1: Float

            when {
                mni == mxi -> {// time is standing still
                    val data = getMaxAmplitudesSync(mnI, s0)
                    a0 = data.left.toFloat()
                    a1 = data.right.toFloat()
                }
                mnI == mxI -> {// time is roughly standing still
                    // from the same index, so 50:50
                    val data0 = getMaxAmplitudesSync(mnI, s0)
                    val data1 = getMaxAmplitudesSync(mxI, s1)
                    a0 = 0.5f * (data0.left + data1.left)
                    a1 = 0.5f * (data0.right + data1.right)
                }
                else -> {// time is changing
                    // sampling from all values
                    // (slow motion sound effects)
                    val data0 = getMaxAmplitudesSync(mnI, s0)
                    val data1 = getMaxAmplitudesSync(mxI, s1)
                    val f0i = 1f - (mni - mnI).toFloat() // x.2f -> 0.8f
                    val f1i = (mxi - mxI).toFloat() // x.2f -> 0.2f
                    var b0 = data0.left * f0i + data1.left * f1i
                    var b1 = data0.right * f0i + data1.right * f1i
                    for (index in mnI + 1 until mxI) {
                        val data = getMaxAmplitudesSync(index, s2)
                        b0 += data.left
                        b1 += data.right
                    }
                    val dt = (mxi - mni).toFloat()
                    // average the values over the time span
                    a0 = b0 / dt
                    a1 = b1 / dt
                }
            }

            // write the data
            leftBuffer[sampleIndex] = a0
            rightBuffer[sampleIndex] = a1

            indexI = indexJ

        }

        return leftBuffer to rightBuffer

    }

}