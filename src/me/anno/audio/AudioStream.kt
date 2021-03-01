package me.anno.audio

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.cache.instances.AudioCache
import me.anno.cache.keys.AudioSliceKey
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.modes.LoopingState
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.Threads.threadWithName
import me.anno.utils.types.Vectors.minus
import me.anno.video.FFMPEGMetadata
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.FFMPEGStream
import org.joml.Vector3f
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// only play once, then destroy; it makes things easier
// (on user input and when finally rendering only)

// todo does not work, if the buffers aren't filled fast enough -> always fill them fast enough...
// todo or restart playing...

/**
 * todo audio effects:
 * done better echoing ;)
 * todo velocity frequency change
 * done pitch
 * todo losing high frequencies in the distance
 * done audio becoming quiet in the distance
 * */
abstract class AudioStream(
    val file: File, val repeat: LoopingState,
    val startTime: Double,
    val meta: FFMPEGMetadata,
    val source: Audio,
    val listener: Camera,
    val playbackSampleRate: Int = 48000
) {

    constructor(audio: Audio, speed: Double, globalTime: Double, playbackSampleRate: Int, listener: Camera) :
            this(
                audio.file,
                audio.isLooping.value,
                0.0,
                getMeta(audio.file, false)!!,
                audio,
                listener,
                playbackSampleRate
            ) {
        configure(audio, speed, globalTime)
    }

    init {
        source.effects.audio = source
    }

    fun configure(audio: Audio, speed: Double, globalTime: Double) {
        globalToLocalTime = { time -> audio.getGlobalTransform(time * speed + globalTime).second }
        val amplitude = audio.amplitude
        val color = audio.color
        localAmplitude = { time -> amplitude[time] * clamp(color[time].w, 0f, 1f) }
    }

    val minPerceptibleAmplitude = 1f / 32500f

    val ffmpegSampleRate = meta.audioSampleRate

    val maxSampleIndex = meta.audioSampleCount

    val ffmpegSliceSampleDuration = 10.0 // seconds, 10s of music
    val ffmpegSliceSampleCount get() = (ffmpegSampleRate * ffmpegSliceSampleDuration).toInt()

    // should be as short as possible for fast calculation
    // should be at least as long as the ffmpeg response time (0.3s for the start of a FHD video)
    companion object {
        val playbackSliceDuration = 1.0
    }

    // map the real time to the correct time xD
    // to do allow skipping and such -> no, too much cleanup ;)

    // var time = 0f

    var isWaitingForBuffer = AtomicBoolean(false)

    var isPlaying = false

    var globalToLocalTime = { globalTime: Double -> globalTime }
    var localAmplitude: (localTime: Double) -> Float = { _: Double -> 1f }

    val buffers = ArrayList<SoundBuffer>()

    fun getAmplitudesSync(index: Double): Pair<Float, Float> {
        if (index < 0f) return 0f to 0f
        // multiply by local time dependent amplitude
        val localAmplitude = localAmplitude(index / ffmpegSampleRate)
        if (localAmplitude < minPerceptibleAmplitude) return 0f to 0f
        val i0 = index.toLong()
        val data0 = getMaxAmplitudesSync(i0)
        val data1 = if (index.toInt().toDouble() == index) { // <3, data doesn't need to be interpolated
            return data0.first * localAmplitude to data0.second * localAmplitude
        } else getMaxAmplitudesSync(i0 + 1)
        val f = (index - i0).toFloat() // sollte ok sein; hohe Präzession ist hier nicht notwendig
        return mix(data0.first, data1.first, f) * localAmplitude to
                mix(data0.second, data1.second, f) * localAmplitude
    }

    fun getMaxAmplitudesSync(index0: Long): Pair<Short, Short> {
        if (index0 < 0 || (repeat == LoopingState.PLAY_ONCE && index0 >= maxSampleIndex)) return 0.toShort() to 0.toShort()
        val index = repeat[index0, maxSampleIndex]
        // val index = if(repeat) index % maxSampleIndex else index
        val sliceIndex = index / ffmpegSliceSampleCount
        val localIndex = (index % ffmpegSliceSampleCount).toInt()
        val arrayIndex0 = localIndex * 2 // for stereo
        val sliceTime = sliceIndex * ffmpegSliceSampleDuration
        val key = AudioSliceKey(file, sliceIndex)
        val timeout = (ffmpegSliceSampleDuration * 2 * 1000).toLong()
        val soundBuffer =
            AudioCache.getEntry(key, timeout, false) {
                val sequence =
                    FFMPEGStream.getAudioSequence(file, sliceTime, ffmpegSliceSampleDuration, ffmpegSampleRate)
                waitUntilDefined { sequence.soundBuffer }
            } as SoundBuffer
        val data = soundBuffer.data!!
        return data[arrayIndex0] to data[arrayIndex0 + 1]
    }

    fun calculateLoudness(global1: Double): AudioTransfer {

        // todo decide on loudness depending on speaker orientation and size (e.g. Nierencharcteristik)
        // todo mix left and right channel depending on orientation and speaker size
        // todo top/bottom (tested from behind) sounds different: because I could hear it
        // todo timing differences seam to matter, so we need to include them (aww)

        val (camLocal2Global, _) = listener.getGlobalTransform(global1)
        val (srcLocal2Global, _) = source.getGlobalTransform(global1)
        val camGlobalPos = camLocal2Global.transformPosition(Vector3f())
        val srcGlobalPos = srcLocal2Global.transformPosition(Vector3f())
        val dirGlobal = (camGlobalPos - srcGlobalPos).normalize() // in global space
        val leftDirGlobal = camLocal2Global.transformDirection(Vector3f(+1f, 0f, -0.1f)).normalize()
        val rightDirGlobal = camLocal2Global.transformDirection(Vector3f(-1f, 0f, -0.1f)).normalize()
        // val distance = camGlobalPos.distance(srcGlobalPos)

        val left1 = leftDirGlobal.dot(dirGlobal) * 0.48 + 0.52
        val right1 = rightDirGlobal.dot(dirGlobal) * 0.48 + 0.52
        return AudioTransfer(left1, right1, 0.0, 0.0)

    }

    val leftPipeline = source.effects.clone()
        .apply { this.camera = listener }
    val rightPipeline = source.effects.clone()
        .apply { this.camera = listener }

    fun requestNextBuffer(startTime: Double, bufferIndex: Long) {

        // "requesting audio buffer $startTime"

        isWaitingForBuffer.set(true)
        threadWithName("AudioStream") {// load all data async

            // "[INFO:AudioStream] Working on buffer $queued"

            // LOGGER.info("$startTime/$bufferIndex")

            // todo speed up for 1:1 playback
            // todo cache sound buffer for 1:1 playback
            // (superfluous calculations)

            // time += dt
            val sampleBuffers = ceil(playbackSampleRate * playbackSliceDuration / bufferSize).toInt()
            val sampleCount = sampleBuffers * bufferSize

            // todo get higher/lower quality, if it's sped up/slowed down?
            // rare use-case...
            // slow motion may be a use case, for which it's worth to request 96kHz or more
            // sound recorded at 0.01x speed is really rare, and at the edge (10Hz -> 10.000Hz)
            // slower frequencies can't be that easily recorded (besides the song/noise of wind (alias air pressure zones changing))

            val dtx = playbackSliceDuration / sampleCount
            val ffmpegSampleRate = ffmpegSampleRate
            val globalToLocalTime = globalToLocalTime
            val localAmplitude = localAmplitude

            val global0 = startTime
            val local0 = globalToLocalTime(global0)
            var index0 = ffmpegSampleRate * local0

            val byteBuffer = ByteBuffer.allocateDirect(sampleCount * 2 * 2)
                .order(ByteOrder.nativeOrder())
            val stereoBuffer = byteBuffer.asShortBuffer()

            var transfer0 = if (source.is3D) calculateLoudness(startTime) else CopyTransfer
            var transfer1 = transfer0

            // todo linear approximation, if possible
            // todo this is possible, if the time is linear, and the amplitude not too crazy, I guess

            val updatePositionEveryNFrames = 100

            val hasPipeline = leftPipeline.stages.isNotEmpty()
            lateinit var leftBuffer: FloatArray
            lateinit var rightBuffer: FloatArray
            if (hasPipeline) {
                leftBuffer = FloatArray(sampleCount)
                rightBuffer = FloatArray(sampleCount)
            }

            for (sampleIndex in 0 until sampleCount) {

                if (sampleIndex % updatePositionEveryNFrames == 0) {

                    // load loudness from camera

                    if (source.is3D) {

                        transfer0 = transfer1
                        val global1 = startTime + (sampleIndex + updatePositionEveryNFrames) * dtx
                        transfer1 = calculateLoudness(global1)

                    }

                }

                val global1 = startTime + (sampleIndex + 1) * dtx
                val local1 = globalToLocalTime(global1)

                val index1 = ffmpegSampleRate * local1

                // average values from index0 to index1
                val mni = min(index0, index1)
                val mxi = max(index0, index1)

                val mnI = mni.toLong()
                val mxI = mxi.toLong()

                var a0: Double
                var a1: Double

                when {
                    mni == mxi -> {// time is standing still
                        val data = getAmplitudesSync(mni)
                        a0 = data.first.toDouble()
                        a1 = data.second.toDouble()
                    }
                    mnI == mxI -> {// time is roughly standing still
                        // from the same index, so 50:50
                        val data0 = getAmplitudesSync(mni)
                        val data1 = getAmplitudesSync(mxi)
                        a0 = 0.5 * (data0.first + data1.first)
                        a1 = 0.5 * (data0.second + data1.second)
                    }
                    else -> {// time is changing
                        // sampling from all values
                        // (slow motion sound effects)
                        val data0 = getAmplitudesSync(mni)
                        val data1 = getAmplitudesSync(mxi)
                        val f0 = 1f - (mni - mnI) // x.2f -> 0.8f
                        val f1 = mxi - mxI // x.2f -> 0.2f
                        var b0 = data0.first * f0 + data1.first * f1
                        var b1 = data0.second * f0 + data1.second * f1
                        for (index in mnI + 1 until mxI) {
                            val data = getMaxAmplitudesSync(index)
                            val time = index.toDouble() / ffmpegSampleRate
                            val amplitude = localAmplitude(time)
                            b0 += amplitude * data.first
                            b1 += amplitude * data.second
                        }
                        val dt = mxi - mni
                        // average the values over the time span
                        a0 = b0 / dt
                        a1 = b1 / dt
                    }
                }

                val approxFraction = (sampleIndex % updatePositionEveryNFrames) * 1.0 / updatePositionEveryNFrames

                // write the data
                val left = transfer0.getLeft(a0, a1, approxFraction, transfer1)
                val right = transfer0.getRight(a0, a1, approxFraction, transfer1)

                if (hasPipeline) {
                    leftBuffer[sampleIndex] = left.toFloat()
                    rightBuffer[sampleIndex] = right.toFloat()
                } else {
                    stereoBuffer.put(doubleToShort(left))
                    stereoBuffer.put(doubleToShort(right))
                }

                index0 = index1

            }

            if (hasPipeline) {

                val global1 = global0 + playbackSliceDuration
                val time0 = Time(local0, global0)

                val local1 = globalToLocalTime(global1)
                val time1 = Time(local1, global1)

                val leftBuffer2 =
                    leftPipeline.process(leftBuffer, Domain.TIME_DOMAIN, Domain.TIME_DOMAIN, time0, time1)
                val rightBuffer2 =
                    rightPipeline.process(rightBuffer, Domain.TIME_DOMAIN, Domain.TIME_DOMAIN, time0, time1)

                for (i in 0 until sampleCount) {
                    stereoBuffer.put(floatToShort(leftBuffer2[i]))
                    stereoBuffer.put(floatToShort(rightBuffer2[i]))
                }
            }

            stereoBuffer.position(0)

            onBufferFilled(stereoBuffer, bufferIndex)

        }

    }

    /**
     * the usual function calls d.toInt().toShort(),
     * which causes breaking from max to -max, which ruins audio quality (cracking)
     * this fixes that :)
     * */
    private fun doubleToShort(d: Double): Short {
        return when {
            d >= 32767.0 -> 32767
            d >= -32768.0 -> d.toInt().toShort()
            else -> -32768
        }
    }

    /**
     * the usual function calls d.toInt().toShort(),
     * which causes breaking from max to -max, which ruins audio quality (cracking)
     * this fixes that :)
     * */
    private fun floatToShort(d: Float): Short {
        return when {
            d >= 32767f -> 32767
            d >= -32768f -> d.toInt().toShort()
            else -> -32768
        }
    }

    abstract fun onBufferFilled(stereoBuffer: ShortBuffer, bufferIndex: Long)

}