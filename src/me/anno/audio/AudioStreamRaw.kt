package me.anno.audio

import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.cache.instances.AudioCache
import me.anno.cache.keys.AudioSliceKey
import me.anno.objects.Audio
import me.anno.objects.Transform
import me.anno.objects.modes.LoopingState
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.io.FileReference
import me.anno.utils.hpc.ProcessingGroup
import me.anno.video.AudioCreator.Companion.playbackSampleRate
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGStream.Companion.getAudioSequence
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioStreamRaw(
    val file: FileReference,
    val repeat: LoopingState,
    val meta: FFMPEGMetadata,
    val is3D: Boolean,
    val speed: Double,
    val source: Audio,
    val destination: Transform,
    val playbackSampleRate: Int = 48000
) {

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {
        val playbackSliceDuration = bufferSize.toDouble() / playbackSampleRate
        val minPerceptibleAmplitude = 1f / 32500f
        val ffmpegSliceSampleDuration = 30.0 // seconds, 10s of music
        val taskQueue = ProcessingGroup("AudioStream", 0.5f)
    }

    fun globalToLocalTime(time: Double): Double {
        return source.getLocalTimeFromRoot(time, false)
    }

    fun localAmplitude(time: Double): Float {
        return source.amplitude[time] * clamp(source.color[time].w(), 0f, 1f)
    }

    val ffmpegSampleRate = meta.audioSampleRate
    val maxSampleIndex = meta.audioSampleCount

    val ffmpegSliceSampleCount = (ffmpegSampleRate * ffmpegSliceSampleDuration).toInt()

    /*class FloatPair(var left: Float, var right: Float) {

        constructor() : this(0f, 0f)

        fun set(left: Float, right: Float): FloatPair {
            this.left = left
            this.right = right
            return this
        }
    }*/

    class ShortPair(var left: Short, var right: Short) {

        constructor() : this(0, 0)

        fun set(left: Short, right: Short): ShortPair {
            this.left = left
            this.right = right
            return this
        }

    }

    /*private fun getAmplitudesSync(index: Double, floatPair: FloatPair, shortPair: ShortPair): FloatPair {
        if (index < 0f) return floatPair.set(0f, 0f)
        // multiply by local time dependent amplitude
        val localAmplitude = localAmplitude(index / ffmpegSampleRate)
        if (localAmplitude < minPerceptibleAmplitude) return floatPair.set(0f, 0f)
        val i0 = index.toLong()
        val data0: ShortPair = getMaxAmplitudesSync(i0, shortPair)
        if (index.toInt().toDouble() == index) { // <3, data doesn't need to be interpolated
            return floatPair.set(data0.left * localAmplitude, data0.right * localAmplitude)
        }
        val firstLeft = data0.left
        val firstRight = data0.right
        val data1: ShortPair = getMaxAmplitudesSync(i0 + 1, shortPair)
        val f = (index - i0).toFloat() // sollte ok sein; hohe Präzession ist hier nicht notwendig
        return floatPair.set(
            mix(firstLeft, data1.left, f) * localAmplitude,
            mix(firstRight, data1.right, f) * localAmplitude
        )
    }*/

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

    private val v0 = Vector3f()
    private val v1 = Vector3f()

    private fun calculateLoudness(globalTime: Double, t0: SimpleTransfer): AudioTransfer {

        // todo decide on loudness depending on speaker orientation and size (e.g. Nierencharcteristik)
        // todo mix left and right channel depending on orientation and speaker size
        // todo top/bottom (tested from behind) sounds different: because I could hear it
        // todo timing differences seam to matter, so we need to include them (aww)

        val localTime = globalToLocalTime(globalTime)
        val amplitude = abs(localAmplitude(localTime))
        if (amplitude < minPerceptibleAmplitude) {
            return ZeroTransfer
        }

        if (!is3D) return t0.set(amplitude, amplitude)

        val (dstLocal2Global, _) = destination.getGlobalTransformTime(globalTime)
        val (srcLocal2Global, _) = source.getGlobalTransformTime(globalTime)
        val dstGlobalPos = dstLocal2Global.transformPosition(Vector3f())
        val srcGlobalPos = srcLocal2Global.transformPosition(Vector3f())
        val dirGlobal = dstGlobalPos.sub(srcGlobalPos).normalize() // in global space
        val leftDirGlobal = dstLocal2Global.transformDirection(v0.set(+1f, 0f, -0.1f)).normalize()
        val rightDirGlobal = dstLocal2Global.transformDirection(v1.set(-1f, 0f, -0.1f)).normalize()
        // val distance = camGlobalPos.distance(srcGlobalPos)

        val left1 = leftDirGlobal.dot(dirGlobal) * 0.48f + 0.52f
        val right1 = rightDirGlobal.dot(dirGlobal) * 0.48f + 0.52f
        return t0.set(left1 * amplitude, right1 * amplitude)

    }

    fun getTime(bufferIndex: Long) = speed * bufferIndex.toDouble() * bufferSize / playbackSampleRate

    fun getBuffer(
        bufferIndex: Long
    ): Pair<FloatArray, FloatArray> {

        val startTime = getTime(bufferIndex)
        val endTime = getTime(bufferIndex + 1)

        // "[INFO:AudioStream] Working on buffer $queued"
        // LOGGER.info("$startTime/$bufferIndex")

        // todo speed up for 1:1 playback
        // todo cache sound buffer for 1:1 playback
        // (superfluous calculations)

        // time += dt
        val sampleCount = bufferSize

        // todo get higher/lower quality, if it's sped up/slowed down?
        // rare use-case...
        // slow motion may be a use case, for which it's worth to request 96kHz or more
        // sound recorded at 0.01x speed is really rare, and at the edge (10Hz -> 10.000Hz)
        // slower frequencies can't be that easily recorded (besides the song/noise of wind (alias air pressure zones changing))

        val dtx = (endTime - startTime) / sampleCount
        val ffmpegSampleRate = ffmpegSampleRate

        val local0 = globalToLocalTime(startTime)
        var index0 = ffmpegSampleRate * local0

        val transfer0 = calculateLoudness(startTime, SimpleTransfer(0f, 0f))

        // will be in first iteration
        var local1 = local0
        var index1 = index0
        val transfer1 = SimpleTransfer(0f, 0f)

        // todo linear approximation, if possible
        // todo this is possible, if the time is linear, and the amplitude not too crazy, I guess

        val updateInterval = 1024
        val updateMask = updateInterval - 1

        val leftBuffer = FloatArray(sampleCount)
        val rightBuffer = FloatArray(sampleCount)

        val s0 = ShortPair()
        val s1 = ShortPair()
        val s2 = ShortPair()

        var fraction = 0.0
        val deltaFraction = 1.0 / updateInterval

        var sampleIndex = -1
        var indexI = ffmpegSampleRate * local0

        while (++sampleIndex < sampleCount) {

            if (sampleIndex and updateMask == 0) {

                // load loudness from camera

                transfer0.set(transfer1)
                index0 = index1

                val global1 = startTime + (sampleIndex + updateInterval + 1) * dtx
                local1 = globalToLocalTime(global1)

                transfer1.set(calculateLoudness(global1, transfer1))
                index1 = ffmpegSampleRate * local1

                if (transfer0.isZero() && transfer1.isZero()) {

                    // there is no audio here -> skip this interval
                    sampleIndex += updateInterval - 1
                    continue

                } else {

                    indexI = index0
                    fraction = deltaFraction

                }

            } else {

                fraction += deltaFraction

            }

            val indexJ = mix(index0, index1, fraction)

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

            val approxFraction = (sampleIndex % updateInterval) * 1f / updateInterval

            // write the data
            leftBuffer[sampleIndex] = transfer0.getLeft(a0, a1, approxFraction, transfer1)
            rightBuffer[sampleIndex] = transfer0.getRight(a0, a1, approxFraction, transfer1)

            indexI = indexJ

        }

        return leftBuffer to rightBuffer

    }

}